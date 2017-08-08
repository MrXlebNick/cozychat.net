package com.messiah.messenger.helpers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.fragment.MessageFragment;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.model.User;
import com.messiah.messenger.utils.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;

import xdroid.toaster.Toaster;

import static com.messiah.messenger.Constants.MESSAGE_FILE_INDEX_PREFIX;

/**
 * Created by XlebNick for CMessenger.
 */

public class ServerHelper extends Observable implements ChatMessageListener {


    public static final String MESSAGE_RECEIVED = "cozychat.net.MESSAGE_RECEIVED";
    public static final String FROM_EXTRA = "cozychat.net.FROM_EXTRA";
    public static final String TIME_EXTRA = "cozychat.net.DATE_EXTRA";
    public static final String MESSAGE_EXTRA = "cozychat.net.MESSAGE_EXTRA";
    private static final String SERVICE_NAME = "ec2-35-165-67-249.us-west-2.compute.amazonaws.com";
    private static ServerHelper mInstance;
    private String mLogin;
    private AbstractXMPPConnection mConnection;
    private Context mContext;
    private int loginTrials = 0;

    private int getUsersTrials = 0;

    private ChatManagerListener chatListener;
//    private FileTransferManager transferManager;

//    private FileListener fileListener;
//
//    private List<FileTransfer> fileTransfers = new ArrayList<>();
//    private boolean isFileDownloading;

    private ServerHelper(String login) {
        mLogin = login;
        initConnection(login);
//        fileListener = new FileListener();
    }

    public static ServerHelper getInstance(Context context) {
        return getInstance(context, null);
    }

    public static ServerHelper getInstance(Context context, String login) {
        if (mInstance == null) {
            String savedLogin = Utils.getPhoneNumber(context);
            if (savedLogin == null) {
                if (login == null) {
                    throw new NullPointerException("No saved login was found and argument login is null");
                }
                mInstance = new ServerHelper(login);
            } else {
                mInstance = new ServerHelper(savedLogin);
            }
        }

        mInstance.mContext = context;
        return mInstance;
    }

    public void initConnection(final String login) {
        if (mConnection == null) {
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(login, login)
                    .setServiceName(SERVICE_NAME)
                    .setHost(SERVICE_NAME)
                    .setConnectTimeout(10000)
                    .setPort(5222)
                    .setDebuggerEnabled(true)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .build();

            SmackConfiguration.DEBUG = true;

            SASLMechanism mechanism = new SASLDigestMD5Mechanism();
            SASLAuthentication.registerSASLMechanism(mechanism);
            SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
            SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
            SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
            mConnection = new XMPPTCPConnection(config);
            DeliveryReceiptManager.getInstanceFor(mConnection).autoAddDeliveryReceiptRequests();

            mConnection.addConnectionListener(new ConnectionListener() {

                @Override
                public void connected(XMPPConnection connection) {
                    Log.i("***", "connected.");
                }

                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {
                    Log.i("***", "authenticated.");
                }

                @Override
                public void connectionClosed() {
                    Log.i("***", "XMPP connection was closed.");
                    initConnection(login);
                }

                @Override
                public void connectionClosedOnError(Exception arg0) {
                    Log.i("***", "Connection to XMPP server was lost.");
                    initConnection(login);
                }

                @Override
                public void reconnectionSuccessful() {
                    Log.i("***", "Successfully reconnected to the XMPP server.");

                }

                @Override
                public void reconnectingIn(int seconds) {
                    Log.i("***", "Reconnecting in " + seconds + " seconds.");
                }

                @Override
                public void reconnectionFailed(Exception arg0) {
                    Log.i("***", "Failed to reconnect to the XMPP server.");
                }
            });
        }


        try {
            loginOrRegister();
        } catch (XMPPException | SmackException | IOException e) {
            e.printStackTrace();
        }
        setMessageListener();
        DeliveryReceiptManager.getInstanceFor(mConnection).addReceiptReceivedListener(new ReceiptReceivedListener() {
            @Override
            public void onReceiptReceived(String fromJid, String toJid, String deliveryReceiptId, Stanza stanza) {
                Log.d("***", "onReceiptReceived: from: " + fromJid + " to: " + toJid + " deliveryReceiptId: " + deliveryReceiptId + " stanza: " + stanza);
            }

        });


    }

    public void loginOrRegister() throws IOException, XMPPException, SmackException {
        try {
            if (!mConnection.isConnected())
                mConnection.connect();
            loginTrials = 0;
        } catch (SASLErrorException e) {


            e.printStackTrace();

            AccountManager accountManager = AccountManager.getInstance(mConnection);
            try {
                accountManager.sensitiveOperationOverInsecureConnection(true);
                accountManager.createAccount(mLogin, mLogin);
                mConnection.disconnect();
                loginOrRegister();
            } catch (XMPPException | IOException | SmackException e1) {
                e1.printStackTrace();
                throw e1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            if (!mConnection.isAuthenticated()) {

                mConnection.login(mLogin, mLogin, null);
                Log.d("***", "login as good boys do");
            }
            loginTrials = 0;
        } catch (SASLErrorException e) {
            e.printStackTrace();

            AccountManager accountManager = AccountManager.getInstance(mConnection);
            try {
                accountManager.sensitiveOperationOverInsecureConnection(true);
                accountManager.createAccount(mLogin, mLogin);
                loginOrRegister();
            } catch (XMPPException | IOException | SmackException e1) {
                e1.printStackTrace();
                throw e1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public HashMap<String, String> getUserProperties() throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occured: " + e.getMessage());
            }
        }
        VCard vCard = VCardManager.getInstanceFor(mConnection).loadVCard();
        HashMap<String, String> properties = new HashMap<>();

        properties.put("username", vCard.getNickName());
        properties.put("phone", mLogin);
        properties.put("email", vCard.getEmailHome());
        properties.put("sip", vCard.getField("sip"));
        return properties;


    }

    public void setUserProperties(HashMap<String, String> properties)
            throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException,
            SmackException.NoResponseException {
        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occured: " + e.getMessage());
            }
        }

        VCard vCard = new VCard();
        vCard.setNickName(properties.get("username"));
        vCard.setEmailHome(properties.get("email"));
        VCardManager.getInstanceFor(mConnection).saveVCard(vCard);
    }

    public List<User> getAllUsers() {
        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occured: " + e.getMessage());
            }
        }
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
                    if (row.getValues("Email") != null)
                        user.mSipNumber = row.getValues("Email").toString().replaceAll("[\\[\\]]", "");
                    users.add(user);
                }

                getUsersTrials = 0;
                return users;
            } else {
                Log.d("***", "No result found");
            }


        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException e) {
            getUsersTrials++;
            if (getUsersTrials > 5) {
                getUsersTrials = 0;
                return null;
            }
            e.printStackTrace();
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
            try {
                loginOrRegister();
            } catch (IOException | XMPPException | SmackException e) {
                e.printStackTrace();

            }
        }
        return null;
    }

    public void setMessageListener() {

        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        Log.d("shit", "setMessageListener " + mConnection.toString() + " " + chatManager.toString());
//        setReceiveFileListener();
        if (chatListener == null) {
            chatListener = new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {
                    Log.d("shit", "chatCreated");
                    chat.addMessageListener(ServerHelper.this);
                }
            };
        }
        try {
            chatManager.removeChatListener(chatListener);
        } catch (NullPointerException e) {
            e.printStackTrace();
            // show message and don't give any fuck
        }
        chatManager.addChatListener(chatListener);
    }

    public void sendMessage(String mPhoneNumber, String messageString) {
        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occurred: " + e.getMessage());
                return;
            }
        }

        org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
        message.setBody(messageString);

        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        try {
            Chat chat =
                    chatManager.createChat(mPhoneNumber + "@" + SERVICE_NAME);

            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);


            chat.sendMessage(message);
            Message messageForDb = new Message();
            messageForDb.sender = Utils.getPhoneNumber(mContext);
            messageForDb.receiver = mPhoneNumber;
            messageForDb.body = messageString;
            messageForDb.time = System.currentTimeMillis();
            messageForDb.isFromMe = true;
            messageForDb.messageId = deliveryReceiptId;
            try {
                messageForDb.body = URLDecoder.decode(message.getBody(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            messageForDb.save();

        } catch (Exception e) {
            e.printStackTrace();
            Toaster.toast("Error while sending message: " + e.getMessage());
        }
    }

    public void saveGcmToken(String token) {
        try {
            VCard vCard = VCardManager.getInstanceFor(mConnection)
                    .loadVCard();
            Log.d("***", "magic is gong to happen" + new Gson().toJson(vCard));
            vCard.setField("GCMID", token);
//            VCardManager.getInstanceFor(mConnection).saveVCard(vCard);
            vCard.save(mConnection);
            Log.d("***", "magic happened");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message localMessage) {
        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occurred: " + e.getMessage());
                return;
            }
        }
        org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
        message.setBody(localMessage.body);

        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        try {
            Chat chat =
                    chatManager.createChat(localMessage.receiver + "@" + SERVICE_NAME);

            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);


            chat.sendMessage(message);
            localMessage.messageId = deliveryReceiptId;
            localMessage.save();
        } catch (Exception e) {
            e.printStackTrace();
            Toaster.toast("Error while sending message: " + e.getMessage());
        }


    }

    //    private void receiveFile() {
//        // TODO Auto-generated method stub
//        if (mConnection != null) {
//
//            FileTransferManager manager = FileTransferManager.getInstanceFor(mConnection);
//
//            manager.addFileTransferListener(new FileTransferListener() {
//
//                public void fileTransferRequest(final FileTransferRequest request) {
//                    new Thread() {
//
//                        @Override
//                        public void run() {
//                            IncomingFileTransfer transfer = request.accept();
//                            File mf = Environment.getExternalStorageDirectory();
//                            final File file = new File(mf.getAbsoluteFile() + "/" + transfer.getFileName());
//                            try {
//                                transfer.recieveFile(file);
//                                while (!transfer.isDone()) {
//                                    try {
//                                        Thread.sleep(1000);
//                                    } catch (Exception e) {
//                                        Log.e("", e.getMessage());
//                                    }
//                                    if (transfer.getStatus().equals(FileTransfer.Status.error)) {
//                                        Log.e("ERROR!!! ", transfer.getError() + "");
//                                    }
//                                    if (transfer.getException() != null) {
//                                        transfer.getException().printStackTrace();
//                                    }
//                                }
//                                Handler handler = new Handler();
//                                handler.post(new Runnable() {
//
//                                    @Override
//                                    public void run() {
//                                        // TODO Auto-generated method stub
////                                        String xMsg = textViewSent.getText().toString();
//                                        String newMessage = "File Received at " + file.getAbsolutePath();
//                                        Log.d("***", newMessage);
////                                        textViewSent.setText("\n"+xMsg+"\n"+newMessage);
//                                    }
//                                });
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        ;
//                    }.start();
//                }
//            });
//        }
//
//    }
//
//
//    public OutgoingFileTransfer sendFile(final Message message) {
//
//        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
//        if (sdm == null) {
//            sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
//        }
//        sdm.addFeature("http://jabber.org/protocol/disco#info");
//        sdm.addFeature("jabber:iq:privacy");
//
//        if (transferManager == null) {
//            transferManager = FileTransferManager.getInstanceFor(mConnection);
//        }
//
//        Log.d("***", mConnection.getUser().substring(mConnection.getUser().indexOf("/" ) + 1));
//
//        final OutgoingFileTransfer transfer = transferManager.createOutgoingFileTransfer(
//                XmppStringUtils.completeJidFrom(message.receiver, SERVICE_NAME, "mobile"));
//
//        final File file = new File(message.body);
//        Log.d("***", message.body);
////        transfer.setResponseTimeout();
//
//        try {
//            transfer.sendFile(file, "GeeChat received media");
//            message.fileStatus = Message.FILE_STATUS_LOADING;
//            message.messageId = transfer.getStreamID();
//            message.update();
//            fileTransfers.add(transfer);
//        } catch (SmackException e) {
//            Log.d("***", "exception");
//            message.fileStatus = Message.FILE_STATUS_FAILED;
//            message.messageId = transfer.getStreamID();
//            message.update();
//            e.printStackTrace();
//        }
//        new Thread(new FileStatusRunnable(message, transfer)).start();
//
//        return transfer;
//    }
//
//        public void setReceiveFileListener() {
//
////
//        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
//        if (sdm == null) {
//            sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
//        }
//        sdm.addFeature("http://jabber.org/protocol/disco#info");
//        sdm.addFeature("jabber:iq:privacy");
//        transferManager = FileTransferManager.getInstanceFor(mConnection);
//
//        transferManager.addFileTransferListener(fileListener);
////        receiveFile();
//
//    }
    @Override
    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
        String body = "";
        try {
            body = URLDecoder.decode(message.getBody(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Message messageForDb = new Message();

        if (body.contains(MESSAGE_FILE_INDEX_PREFIX)) {
            String[] parts = body.split(Pattern.quote(MESSAGE_FILE_INDEX_PREFIX));
            messageForDb.body = parts[0];
            messageForDb.fileName = parts[1];
            messageForDb.type = Constants.MESSAGE_TYPE_FILE;

        } else {

            messageForDb.type = Constants.MESSAGE_TYPE_TEXT;
            messageForDb.body = body;
        }

        messageForDb.sender = message.getFrom().substring(0, message.getFrom().indexOf("@"));
        messageForDb.isFromMe = false;
        messageForDb.time = System.currentTimeMillis();
        messageForDb.messageId = message.getStanzaId();
        messageForDb.receiver = Utils.getPhoneNumber(mContext);

        Log.d("***", messageForDb.body + " " + messageForDb.sender + " " + message.getStanzaId());
        if (!TextUtils.isEmpty(messageForDb.body)) {
            messageForDb.save();
            if (!MessageFragment.isActive) {
                Utils.sendNotification(mContext);
            }
            setChanged();
            notifyObservers();
        }

        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(mContext);

        Intent intent = new Intent(MESSAGE_RECEIVED);
        if (message != null)
            intent.putExtra(MESSAGE_EXTRA, messageForDb.body);
        intent.putExtra(FROM_EXTRA, messageForDb.receiver);
        intent.putExtra(TIME_EXTRA, messageForDb.time);
        broadcaster.sendBroadcast(intent);

    }

    public void sendFileMessage(Message mItem, String messageString) {
        if (!mConnection.isConnected()) {
            try {
                loginOrRegister();
            } catch (SmackException e) {
                e.printStackTrace();
                if (e instanceof SmackException.ConnectionException) {
                    loginTrials = 0;
                    Toaster.toast("Network error, try later");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.toast("An error occurred: " + e.getMessage());
                return;
            }
        }

        org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
        message.setBody(messageString);

        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        try {
            Chat chat =
                    chatManager.createChat(mItem.receiver + "@" + SERVICE_NAME);

            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);


            chat.sendMessage(message);
            mItem.messageId = deliveryReceiptId;
            mItem.body = messageString;
            mItem.fileKey = messageString.split(Constants.MESSAGE_FILE_INDEX_PREFIX)[0];
            mItem.update();

        } catch (Exception e) {
            e.printStackTrace();
            Toaster.toast("Error while sending message: " + e.getMessage());
        }
    }

//    public FileTransfer getIncomingFileTrnsfer(String messageId) {
//        for ( FileTransfer transfer : fileTransfers)
//            if (transfer.getStreamID().equals(messageId))
//                return transfer;
//        return null;
//    }

//    public class FileListener implements FileTransferListener {
//        private boolean isFileDownloading = false;
//        @Override
//        public void fileTransferRequest(FileTransferRequest request) {
//            if (isFileDownloading)
//                return;
//
//            if (getIncomingFileTrnsfer(request.getStreamID()) != null)
//                return;
//
//            IncomingFileTransfer transfer = request.accept();
//            fileTransfers.add(transfer);
//
//
//            try {
//                File file = new File(Environment.getExternalStorageDirectory() +"/"+ request.getFileName());
//
//                final Message message = new Message();
//                message.isFromMe = false;
//                message.body = file.getAbsolutePath();
//                message.time = System.currentTimeMillis();
//                message.messageId = transfer.getStreamID();
//                message.receiver = request.getRequestor().split("@")[0];
//                message.sender = Utils.getPhoneNumber(getContext());
//                message.type = Message.TYPE_FILE;
//                message.save();
//
//                setChanged();
//                notifyObservers();
//
//                transfer.recieveFile(file);
//
//                new Thread(new FileStatusRunnable(message, transfer)).start();
//
////
////                    InputStream is = transfer.recieveFile();
////                    ByteArrayOutputStream os = new ByteArrayOutputStream();
////                    int nRead;
////                    byte[] buf = new byte[1024];
////                    FileOutputStream outputStreamWriter = new FileOutputStream(file);
////                    while ((nRead = is.read(buf,  0, buf.length)) != -1) {
//////                        os.write(buf, 0, nRead);
////                        outputStreamWriter.write(buf, 0, nRead);
////                    }
////
////                    outputStreamWriter.close();
////
////                    os.flush();
//
//            } catch (SmackException | IOException e) {
//                e.printStackTrace();
//                if (e instanceof SmackException){
//
//                    SmackException exception = (SmackException) e;
//                    ExecutionException executionException = (ExecutionException) exception.getCause();
//                    if (executionException != null) { // this should be not null
//                        Throwable causingException = executionException.getCause();
//                        causingException.printStackTrace(); // or log
//                    }
//                }
//
//            }
//        }
//    }

//    private class FileStatusRunnable implements Runnable {
//
//        private final Message message;
//        private final FileTransfer transfer;
//
//        FileStatusRunnable(Message message, FileTransfer transfer){
//            this.message = message;
//            this.transfer = transfer;
//        }
//        @Override
//        public void run() {
//            do {
//                Log.d("***", transfer.getStatus() + " " + " " + transfer.getError() + " " + transfer.getProgress());
//                if ((transfer.getStatus().equals(FileTransfer.Status.in_progress) ||
//                        transfer.getStatus().equals(FileTransfer.Status.negotiated )||
//                        transfer.getStatus().equals(FileTransfer.Status.negotiating_stream ) ||
//                        transfer.getStatus().equals(FileTransfer.Status.negotiating_transfer)) &&
//                        !message.fileStatus.equals(Message.FILE_STATUS_LOADING)){
//                    message.fileStatus = Message.FILE_STATUS_LOADING;
//                    message.update();
//                }
//            } while (!transfer.isDone());
//            if (transfer.isDone() && transfer.getStatus() == FileTransfer.Status.complete){
//
//                message.fileStatus = Message.FILE_STATUS_LOADED;
//                message.update();
//            } else {
//
//                message.fileStatus = Message.FILE_STATUS_FAILED;
//                message.update();
//            }
//        }
//    }
}
