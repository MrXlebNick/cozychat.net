package net.xlebnick.geechat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import net.xlebnick.geechat.model.Message;
import net.xlebnick.geechat.model.User;
import net.xlebnick.geechat.utils.Utils;

import static net.xlebnick.geechat.utils.ServerHelper.FROM_EXTRA;
import static net.xlebnick.geechat.utils.ServerHelper.MESSAGE_EXTRA;
import static net.xlebnick.geechat.utils.ServerHelper.MESSAGE_RECEIVED;
import static net.xlebnick.geechat.utils.ServerHelper.TIME_EXTRA;

/**
 * Created by XlebNick for CMessenger.
 */
public class SmackConnection implements ConnectionListener, ChatManagerListener, RosterListener, ChatMessageListener, PingFailedListener {

    private Gson gson;
    private AsyncTask<Void, Void, Void> mRegisterTask;
    private FileTransferManager manager;
    private static final String TAG = "SMACK";
    public  Context mApplicationContext;
    public static SmackConnection instance = null;
    private final String mServiceName = "cozychat.net";
    private final String mServiceAddress = "78.46.85.86";
    private static XMPPTCPConnection mConnection;
    private static final byte[] dataToSend = StringUtils.randomString(1024 * 4 * 3).getBytes();
    private static byte[] dataReceived;
    private XMPPTCPConnectionConfiguration.Builder config;
    private String mLogin;

    public void init(String mUsername, String mPassword) {
        mLogin = mUsername;
        config = XMPPTCPConnectionConfiguration.builder();
        config.setServiceName(mServiceName);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setHost(mServiceAddress);
        config.setPort(5222);
        config.setCompressionEnabled(true);
        config.setUsernameAndPassword(mUsername, mPassword);
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        config.setCompressionEnabled(true);
        try {
            TLSUtils.acceptAllCertificates(config);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        mConnection = new XMPPTCPConnection(config.build());
        mConnection.addConnectionListener(this);
        PingManager pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);
        ChatManager.getInstanceFor(mConnection).addChatListener(this);
        manager = FileTransferManager.getInstanceFor(mConnection);
        manager.addFileTransferListener(new FileTransferIMPL());
        FileTransferNegotiator.getInstanceFor(mConnection);

        gson = new Gson();
        connectAndLoginAnonymously();

    }

    public void connectAndLoginAnonymously() {
        mRegisterTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mConnection.disconnect();
                    mConnection.connect();
                    mConnection.login();
                } catch (SmackException | XMPPException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void res) {
            }
        };

        // execute AsyncTask
        mRegisterTask.execute(null, null, null);
    }


    public void login(final String username, final String password) {
        mRegisterTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    disconnect();
                    config.setUsernameAndPassword(username, password);

                    mConnection.connect();

                    mConnection.login();

                } catch (SmackException | XMPPException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void res) {
            }
        };
        // execute AsyncTask
        mRegisterTask.execute(null, null, null);
    }


    public void disconnect() {
        Log.i(TAG, "disconnect()");
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    public List<User> getAllUsers() {
        try {
            UserSearchManager manager = new UserSearchManager(mConnection);
            String searchFormString = "search." + mConnection.getServiceName();
            Form searchForm;

            searchForm = manager.getSearchForm(searchFormString);

            Form answerForm = searchForm.createAnswerForm();

            UserSearch userSearch = new UserSearch();
            answerForm.setAnswer("Username", true);
            answerForm.setAnswer("search", "*");

            ReportedData results = userSearch.sendSearchForm(mConnection, answerForm, searchFormString);
            if (results != null) {
                List<User> users = new ArrayList<>();
                List<ReportedData.Row> rows = results.getRows();
                for (ReportedData.Row row : rows) {
                    User user = new User();
                    user.mPhoneNumber = row.getValues("Username").toString().replaceAll("[\\[\\]]", "");
                    if (row.getValues("Name") != null)
                        user.mFullName = row.getValues("Name").toString().replaceAll("[\\[\\]]", "");
                    if (!user.mPhoneNumber.equals(mLogin))
                        users.add(user);
                }
                VCard card = VCardManager.getInstanceFor(mConnection).loadVCard("xlebnick@cozychat.net");
                card.setPhoneWork("VOICE", "6001");
                card.save(mConnection);
                return users;
            } else {
                Log.d("***", "No result found");
            }

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            connectAndLoginAnonymously();
            return getAllUsers();
        }
        return null;
    }


    public void sendMessage(Message chatMessage) {
        gson = new Gson();
        Log.i(TAG, "sendMessage()");
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(chatMessage.receiver + "@cozychat.net", this);
//        Gson gson = new Gson();
//        String body = gson.toJson(chatMessage);
        final org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
        message.setBody(chatMessage.body);
        message.setStanzaId(chatMessage.messageId);
        message.setType(org.jivesoftware.smack.packet.Message.Type.chat);
        try {
            chat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Log.i(TAG, "chatCreated()");
        chat.addMessageListener(this);
    }

    //MessageListener
    @Override
    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
        gson = new Gson();
        Log.i(TAG, "processMessage()");
        if (message.getType().equals(org.jivesoftware.smack.packet.Message.Type.chat) || message.getType().equals(org.jivesoftware.smack.packet.Message.Type.normal)) {
            Log.i("MyXMPP_MESSAGE_LISTENER", "Xmpp Message received: '"
                    + message);
            if (message.getType() == org.jivesoftware.smack.packet.Message.Type.chat
                    && message.getBody() != null) {
                String sender1 = message.getFrom();
                final Random random = new Random();
                final String delimiter = "\\@";
                String[] temp = sender1.split(delimiter);
                final String sender = temp[0];
                Log.d("shit", gson.toJson(message));
                final Message chatMessage = new Message();
                chatMessage.body = message.getBody();
                chatMessage.time = System.currentTimeMillis();
                chatMessage.messageId = " " + random.nextInt(1000);
                processMessage(sender, chatMessage);
            }
        }
    }

    public void processMessage(final String sender, Message message) {
        message.sender = sender;
        message.receiver = Utils.getPhoneNumber(mApplicationContext);
        message.sender = sender;
        message.type = "TEXT";
        message.isFromMe = false;
        Log.i("MSG RECE", message.body + " " + message.receiver + " " + message.sender);
        message.save();
        Log.i("MSG RECE", "Added");

        notifyAboutNewMessage(message);

    }

    private void notifyAboutNewMessage(Message message){


        Utils.sendNotification(mApplicationContext);

        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(mApplicationContext);

        Intent intent = new Intent(MESSAGE_RECEIVED);
        if(message != null)
            intent.putExtra(MESSAGE_EXTRA, message.body);
        intent.putExtra(FROM_EXTRA, message.receiver);
        intent.putExtra(TIME_EXTRA, message.time);
        broadcaster.sendBroadcast(intent);
    }


    private void processMessage(final FileTransferRequest request) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Log.i("MSG RECE", "LOOPER");
                int iend = request.getRequestor().lastIndexOf("@");
                String requester = request.getRequestor().substring(0, iend);
                Log.i("MSG RECE", requester);
                Message message = new Message();
                message.sender = requester;
                message.receiver = Utils.getPhoneNumber(mApplicationContext);

                message.isFromMe = false;
                message.body = request.getFileName();
                message.time = System.currentTimeMillis();
                message.messageId = request.getStreamID();
                message.type = "IMG";
                message.save();
                Log.i("MSG RECE", request.getRequestor());

            }
        });


    }


//ConnectionListener

    @Override
    public void connected(XMPPConnection connection) {

        Log.i(TAG, "connected()");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean arg0) {
        Log.i(TAG, "authenticated()");
    }

    @Override
    public void connectionClosed() {
        Log.i(TAG, "connectionClosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.i(TAG, "connectionClosedOnError()");

    }

    @Override
    public void reconnectingIn(int seconds) {
        Log.i(TAG, "reconnectingIn()");

    }

    @Override
    public void reconnectionSuccessful() {
        Log.i(TAG, "reconnectionSuccessful()");
    }

    @Override
    public void reconnectionFailed(Exception e) {
        Log.i(TAG, "reconnectionFailed()");
    }

//RosterListener

    @Override
    public void entriesAdded(Collection<String> addresses) {

    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {

    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {

    }

    @Override
    public void presenceChanged(Presence presence) {
        Log.i(TAG, "presenceChanged()");
    }


    @Override
    public void pingFailed() {
        Log.i(TAG, "pingFailed()");
    }


    public boolean createNewAccount(String username, String newpassword) {
        boolean status = false;
        if (mConnection == null) {
            try {
                mConnection.connect();
            } catch (SmackException | IOException | XMPPException e) {
                e.printStackTrace();
            }
        }

        try {
            String newusername = username + mConnection.getServiceName();
            AccountManager accountManager = AccountManager.getInstance(mConnection);
            accountManager.createAccount(username, newpassword);
            status = true;
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException e) {
            e.printStackTrace();
            status = false;
        }
        mConnection.disconnect();
        return status;

    }

    public XMPPTCPConnection getConnection() {
        return mConnection;
    }

    private SmackConnection(Context context) {
        mApplicationContext = context;
    }

    private SmackConnection() {
    }

    public XMPPTCPConnection SmackConnection() {
        return mConnection;

    }

    public static SmackConnection getInstance(Context context) {
        if (instance == null) {
            instance = new SmackConnection(context);
        }
        return instance;
    }

    public class FileTransferIMPL implements FileTransferListener {

        @Override
        public void fileTransferRequest(final FileTransferRequest request) {
            final IncomingFileTransfer transfer = request.accept();
            try {
                InputStream is = transfer.recieveFile();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int nRead;
                byte[] buf = new byte[1024];
                try {
                    while ((nRead = is.read(buf, 0, buf.length)) != -1) {
                        os.write(buf, 0, nRead);
                    }
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dataReceived = os.toByteArray();
                createDirectoryAndSaveFile(dataReceived, request.getFileName());
                Log.i("File Received", transfer.getFileName());
                processMessage(request);
            } catch (XMPPException | SmackException ex) {
                ex.printStackTrace();
            }
        }

    }

    public void fileTransfer(String user, Bitmap bitmap, String filename) throws XMPPException {
        Roster roster = Roster.getInstanceFor(mConnection);
        String destination = null;
        destination = roster.getPresence(user).getFrom();
        // Create the file transfer manager
        FileTransferManager manager = FileTransferManager.getInstanceFor(mConnection);
        // Create the outgoing file transfer
        final OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(destination);
        // Send the file
//        transfer.sendFile(new File("abc.txt"), "You won't believe this!");
        transfer.sendStream(new ByteArrayInputStream(convertFileToByte(bitmap)), filename, convertFileToByte(bitmap).length, "A greeting");

        System.out.println("Status :: " + transfer.getStatus() + " Error :: " + transfer.getError() + " Exception :: " + transfer.getException());
        System.out.println("Is it done? " + transfer.isDone());
        if (transfer.getStatus().equals(FileTransfer.Status.refused))
            System.out.println("refused  " + transfer.getError());
        else if (transfer.getStatus().equals(FileTransfer.Status.error))
            System.out.println(" error " + transfer.getError());
        else if (transfer.getStatus().equals(FileTransfer.Status.cancelled))
            System.out.println(" cancelled  " + transfer.getError());
        else
            System.out.println("Success");
    }

    public byte[] convertFileToByte(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void createDirectoryAndSaveFile(byte[] imageToSave, String fileName) {
        File direct = new File(Environment.getExternalStorageDirectory() + "/LocShopie/Received/");
        if (!direct.exists()) {
            File wallpaperDirectory = new File("/sdcard/LocShopie/Received/");
            wallpaperDirectory.mkdirs();
        }
        File file = new File(new File("/sdcard/LocShopie/Received/"), fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(imageToSave);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
