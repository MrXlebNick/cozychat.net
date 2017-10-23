package com.messiah.messenger.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.fragment.MessageFragment;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.model.User;
import com.messiah.messenger.utils.FileIOApi;
import com.messiah.messenger.utils.FileResponse;
import com.messiah.messenger.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
//import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.messiah.messenger.Constants.MESSAGE_FILE_INDEX_PREFIX;

/**
 * Created by XlebNick for CMessenger.
 */

public class XmppHelper implements IncomingChatMessageListener {

    public static final String MESSAGE_RECEIVED = "cozychat.net.MESSAGE_RECEIVED";
    public static final String FROM_EXTRA = "cozychat.net.FROM_EXTRA";
    public static final String TIME_EXTRA = "cozychat.net.DATE_EXTRA";
    public static final String MESSAGE_EXTRA = "cozychat.net.MESSAGE_EXTRA";
    private static final String SERVICE_NAME = "ec2-18-216-77-83.us-east-2.compute.amazonaws.com";
    private static XmppHelper mInstance;
    private String mLogin;
    private AbstractXMPPConnection mConnection;
    private ChatManagerListener chatListener;
//    private OmemoManager omemoManager;

    private XmppHelper(String login) {
        mLogin = login;
        Log.d("xmpp", login);
        init();
    }

    public static XmppHelper getInstance() {
        return getInstance(null);
    }

    public static XmppHelper getInstance(String login) {
        if (mInstance == null) {

            if (CozyChatApplication.getContext() == null) { throw new NullPointerException(); }

            String savedLogin = Utils.getPhoneNumber(CozyChatApplication.getContext());
            if (savedLogin == null) {
                if (login == null) {
                    throw new NullPointerException(
                            "No saved login was found and argument login is null");
                }
                mInstance = new XmppHelper(login);
            } else {
                mInstance = new XmppHelper(savedLogin);
            }
        }
        return mInstance;
    }

    private void init() {
        Log.d("xmpp", "init");

        XMPPTCPConnectionConfiguration config = null;
        try {
            config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(mLogin, mLogin)
                    .setXmppDomain(SERVICE_NAME)
                    .setHost(SERVICE_NAME)
                    .setConnectTimeout(10000)
                    .setPort(5222)
                    .setDebuggerEnabled(true)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .build();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

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
                Log.i("xmpp", "connected.");
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                Log.i("xmpp", "authenticated.");
            }

            @Override
            public void connectionClosed() {
                Log.i("xmpp", "XMPP connection was closed.");
                init();
            }

            @Override
            public void connectionClosedOnError(Exception arg0) {
                Log.i("xmpp", "Connection to XMPP server was lost.");
                init();
            }

            @Override
            public void reconnectionSuccessful() {
                Log.i("xmpp", "Successfully reconnected to the XMPP server.");

            }

            @Override
            public void reconnectingIn(int seconds) {
                Log.i("xmpp", "Reconnecting in " + seconds + " seconds.");
            }

            @Override
            public void reconnectionFailed(Exception arg0) {
                Log.i("xmpp", "Failed to reconnect to the XMPP server.");
            }
        });

        setMessageListener();

    }

    public io.reactivex.Observable<XMPPConnection> connectrx(){
        if (mConnection == null) {
            init();
        }
        if (mConnection.isConnected())
            return io.reactivex.Observable.just(mConnection);

        Log.d("xmpp", "connect");

        return io.reactivex.Observable.create(emitter -> {
            ConnectionListener listener = new ConnectionListener() {
                @Override
                public void connected(XMPPConnection connection) {
                    emitter.onNext(connection);
                    connection.removeConnectionListener(this);
                }

                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {

                }

                @Override
                public void connectionClosed() {

                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    emitter.onError(e);
                    mConnection.removeConnectionListener(this);
                }

                @Override
                public void reconnectionSuccessful() {

                }

                @Override
                public void reconnectingIn(int seconds) {

                }

                @Override
                public void reconnectionFailed(Exception e) {

                }
            };
            mConnection.addConnectionListener(listener);
            try{
                mConnection.connect();
            } catch (Exception e){
                emitter.onError(e);
            }
        });
    }


//    public void connect() {
//
//        if (mConnection == null) {
//            init();
//        }
//
//        try {
//            loginOrRegister();
//        } catch (XMPPException | SmackException | IOException e) {
//            e.printStackTrace();
//        }
//        setMessageListener();
//        DeliveryReceiptManager.getInstanceFor(mConnection)
//                .addReceiptReceivedListener((fromJid, toJid, deliveryReceiptId, stanza) -> Log.d(
//                        "***",
//                        "onReceiptReceived: from: " + fromJid + " to: " + toJid +
//                                " deliveryReceiptId: " + deliveryReceiptId + " stanza: " + stanza));
//    }



//    public void loginOrRegister() throws IOException, XMPPException, SmackException {
//        try {
//            if (! mConnection.isConnected()) { mConnection.connect(); }
//        } catch (SASLErrorException e) {
//
//
//            e.printStackTrace();
//            AccountManager accountManager = AccountManager.getInstance(mConnection);
//            try {
//                accountManager.sensitiveOperationOverInsecureConnection(true);
//                accountManager.createAccount(mLogin, mLogin);
//                mConnection.disconnect();
//                loginOrRegister();
//            } catch (XMPPException | IOException | SmackException e1) {
//                e1.printStackTrace();
//                throw e1;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//
//            if (! mConnection.isAuthenticated()) {
//
//                mConnection.login(mLogin, mLogin, null);
//                Log.d("***", "login as good boys do");
//            }
//        } catch (SASLErrorException e) {
//
//            Log.d("***", "login asl");
//            e.printStackTrace();
//            AccountManager accountManager = AccountManager.getInstance(mConnection);
//            try {
//                accountManager.sensitiveOperationOverInsecureConnection(true);
//                accountManager.createAccount(mLogin, mLogin);
//                loginOrRegister();
//            } catch (XMPPException | IOException | SmackException e1) {
//                e1.printStackTrace();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        getUserPropertiesrx(mLogin);
//    }
    
    public io.reactivex.Observable<HashMap<String, String>> getUserPropertiesrx(){
        return getUserPropertiesrx(mLogin);
    }
    
    public io.reactivex.Observable<HashMap<String, String>> getUserPropertiesrx(String phoneNumber){

        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(emitter -> {
            VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
            EntityBareJid userJid = JidCreate.entityBareFrom(phoneNumber + "@" + SERVICE_NAME);
            VCard vCard = vCardManager.loadVCard(userJid);
            HashMap<String, String> properties = new HashMap<>();


            properties.put("username", vCard.getNickName());
            properties.put("phone", phoneNumber);
            properties.put("email", vCard.getEmailHome());
            properties.put("niknam", vCard.getField("niknam"));
            properties.put("name", vCard.getField("name"));
            properties.put("date", vCard.getField("date"));
            properties.put("email", vCard.getField("email"));
            properties.put("region", vCard.getField("region"));
            properties.put("country", vCard.getField("country"));
            properties.put("place", vCard.getField("place"));
            properties.put("nativePlace", vCard.getField("nativePlace"));
            properties.put("languages", vCard.getField("languages"));
            properties.put("education1", vCard.getField("education1"));
            properties.put("education2", vCard.getField("education2"));
            properties.put("website", vCard.getField("website"));
            properties.put("phone1", vCard.getField("phone1"));
            properties.put("phone2", vCard.getField("phone2"));
            properties.put("phone3", vCard.getField("phone3"));
            properties.put("gender", vCard.getField("gender"));
            properties.put("avatarKey", vCard.getField("avatarKey"));
            properties.put("avatarFileName", vCard.getField("avatarFileName"));

            String sipNumber = vCard.getField("sip");
            properties.put("sip", sipNumber);
            Log.d("***",
                    phoneNumber + " " + Utils.getPhoneNumber(CozyChatApplication.getContext()) + " " +
                            sipNumber);
            if (phoneNumber.equals(Utils.getPhoneNumber(CozyChatApplication.getContext())) &&
                    ! TextUtils.isEmpty(sipNumber)) {
                Utils.putSipNumber(CozyChatApplication.getContext(), sipNumber);
            }
            return properties;
        });
    }

    private io.reactivex.Observable<XMPPConnection> getConnectedObservable() {
        if (mConnection == null)
            init();
        io.reactivex.Observable<XMPPConnection> observable = io.reactivex.Observable.just(mConnection);
        if (!mConnection.isConnected())
            observable = observable.flatMap(o -> connectrx());
        return observable;
    }

    public io.reactivex.Observable<XMPPConnection> getSignedInObservable() {
        io.reactivex.Observable<XMPPConnection> observable = getConnectedObservable();
        if (!mConnection.isAuthenticated())
            observable = observable.flatMap(o -> loginrx());
        return observable;
    }

//    public HashMap<String, String> getUserPropertiesrx() throws SmackException.NotConnectedException,
//            XMPPException.XMPPErrorException, SmackException.NoResponseException {
//        return getUserProperties(mLogin);
//    }

    public void uploadAvatar(File avatar, Callback<FileResponse> callback){

        RequestBody requestFile;
        try {

            String extension = MimeTypeMap.getFileExtensionFromUrl(avatar.getAbsolutePath());

            requestFile = RequestBody.create(
                    MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)),
                    avatar);
        } catch (Exception e){
            e.printStackTrace();
            String mediaTypeString = CozyChatApplication.getContext().getContentResolver().getType(Uri.fromFile(avatar));
            if (mediaTypeString == null || mediaTypeString.isEmpty()){
                requestFile = RequestBody.create(null, avatar);
            } else {
                requestFile = RequestBody.create(
                        MediaType.parse(CozyChatApplication.getContext().getContentResolver().getType(Uri.fromFile(avatar))),
                        avatar);
            }

        }

        MultipartBody.Part body =
                MultipartBody.Part.createFormData(Constants.FILE_MULTIPART_NAME, avatar.getName(), requestFile);



        FileIOApi retrofit = new Retrofit.Builder().baseUrl("http://ec2-18-216-77-83.us-east-2.compute.amazonaws.com:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(FileIOApi.class);


        retrofit.upload(body).enqueue(callback);


    }

    public io.reactivex.Observable<File> downloadAvatarRx(String avatarKey, String avatarFileName){
        return Observable.create(e -> {
            Callback<ResponseBody> callback = new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                e.onNext(DocumentHelper.writeResponseBodyToDisk(response.body(), avatarFileName));
                                e.onComplete();
                                return null;
                            }


                        }.execute();
                    } else {
                        e.onError(new NullPointerException());
                        Log.d("***", "on response failure ");
//                                setStatus(Message.FILE_STATUS_FAILED);
                    }

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
//                            setStatus(Message.FILE_STATUS_FAILED);
                    e.onError(t);
                    t.printStackTrace();
                }
            };

            FileIOApi retrofit = new Retrofit.Builder().baseUrl("http://ec2-18-216-77-83.us-east-2.compute.amazonaws.com:8080")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(FileIOApi.class);
            retrofit.getFile(avatarKey).enqueue(callback);
        });
    }

    public io.reactivex.Observable<Boolean> setUserPropertiesrx(HashMap<String, String> properties){
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {

            VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
            VCard vCard = vCardManager.loadVCard();

            Log.d("profile", "size " + properties.size());

            for (Map.Entry<String, String> entry : properties.entrySet()){

                Log.d("profile", entry.getKey() + " " + entry.getValue());
                if (!(entry.getKey().equals("niknam") && entry.getValue().equals("NickName")))
                    vCard.setField(entry.getKey(), entry.getValue());
            }
            vCard.save(mConnection);
//            VCardManager.getInstanceFor(mConnection).saveVCard(vCard);
            return true;
        });

    }

//    public HashMap<String, String> getUserProperties(String phoneNumber)
//            throws SmackException.NotConnectedException,
//            XMPPException.XMPPErrorException, SmackException.NoResponseException {
//
//        if (! mConnection.isConnected() || ! mConnection.isAuthenticated()) {
//            try {
//                loginOrRegister();
//                tasksOnLogin.add(() -> {
//                    try {
//                        getUserProperties(phoneNumber);
//                    } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
//                        e.printStackTrace();
//                    }
//                });
//            } catch (SmackException e) {
//                e.printStackTrace();
//                if (e instanceof SmackException.ConnectionException) {
//                    loginTrials = 0;
//                    Toaster.toast("Network error, try later");
//                    return null;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Toaster.toast("An error occured: " + e.getMessage());
//            }
//        }
//
//
//        Log.d("sipka", phoneNumber + "@" + SERVICE_NAME);
//        VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
//        VCard vCard = vCardManager.loadVCard(phoneNumber + "@" + SERVICE_NAME);
//        HashMap<String, String> properties = new HashMap<>();
//
//        properties.put("username", vCard.getNickName());
//        properties.put("phone", phoneNumber);
//        properties.put("email", vCard.getEmailHome());
//
//        String sipNumber = vCard.getField("sip");
//        properties.put("sip", sipNumber);
//        Log.d("***",
//                phoneNumber + " " + Utils.getPhoneNumber(CozyChatApplication.getContext()) + " " +
//                        sipNumber);
//        if (phoneNumber.equals(Utils.getPhoneNumber(CozyChatApplication.getContext())) &&
//                ! TextUtils.isEmpty(sipNumber)) {
//            vCard.setField("sip", "6011");
//            vCard.save(mConnection);
//            Utils.putSipNumber(CozyChatApplication.getContext(), sipNumber);
//            SipHelper.getInstance().register();
//        }
//        return properties;
//    }

//    public List<User> getAllUsers() {
//        if (! mConnection.isConnected()) {
//            try {
//                loginOrRegister();
//            } catch (SmackException e) {
//                e.printStackTrace();
//                if (e instanceof SmackException.ConnectionException) {
//                    loginTrials = 0;
//                    Toaster.toast("Network error, try later");
//                    return null;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Toaster.toast("An error occured: " + e.getMessage());
//            }
//        }
//        try {
//            UserSearchManager manager = new UserSearchManager(mConnection);
//            String searchFormString = "search." + mConnection.getServiceName();
//            Form searchForm;
//
//            searchForm = manager.getSearchForm(searchFormString);
//            Form answerForm = searchForm.createAnswerForm();
//
//            UserSearch userSearch = new UserSearch();
//            answerForm.setAnswer("Username", true);
//            answerForm.setAnswer("search", "*");
//
//            ReportedData results =
//                    userSearch.sendSearchForm(mConnection, answerForm, searchFormString);
//            if (results != null) {
//                List<User> users = new ArrayList<>();
//                List<ReportedData.Row> rows = results.getRows();
//                for (ReportedData.Row row : rows) {
//                    User user = new User();
//                    user.mPhoneNumber =
//                            row.getValues("Username").toString().replaceAll("[\\[\\]]", "");
//                    if (row.getValues("Name") != null) {
//                        user.mFullName =
//                                row.getValues("Name").toString().replaceAll("[\\[\\]]", "");
//                    }
//                    if (row.getValues("Email") != null) {
//                        user.mSipNumber =
//                                row.getValues("Email").toString().replaceAll("[\\[\\]]", "");
//                    }
//                    if (row.getValues("sip") != null) {
//                        user.mSipNumber =
//                                row.getValues("sip").toString().replaceAll("[\\[\\]]", "");
//                    }
//                    users.add(user);
//                }
//
//                getUsersTrials = 0;
//                return users;
//            } else {
//                Log.d("***", "No result found");
//            }
//
//
//        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
//                | SmackException.NotConnectedException e) {
//            getUsersTrials++;
//            if (getUsersTrials > 5) {
//                getUsersTrials = 0;
//                return null;
//            }
//            e.printStackTrace();
//        } catch (IllegalArgumentException e1) {
//            e1.printStackTrace();
//            try {
//                loginOrRegister();
//            } catch (IOException | XMPPException | SmackException e) {
//                e.printStackTrace();
//
//            }
//        }
//        return null;
//    }

    public io.reactivex.Observable<List<User>> getAllUsersrx() {
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {

            UserSearchManager manager = new UserSearchManager(mConnection);
            String searchFormString = "search." + mConnection.getServiceName();
            Form searchForm;

            DomainBareJid searchJid = JidCreate.domainBareFrom("search." + mConnection.getServiceName());

            searchForm = manager.getSearchForm(searchJid);
            Form answerForm = searchForm.createAnswerForm();

            UserSearch userSearch = new UserSearch();
            answerForm.setAnswer("Username", true);
            answerForm.setAnswer("search", "*");

            ReportedData results =
                    userSearch.sendSearchForm(mConnection, answerForm, searchJid);
            List<User> users = new ArrayList<>();
            List<ReportedData.Row> rows = results.getRows();
            for (ReportedData.Row row : rows) {
                User user = new User();
                user.mPhoneNumber =
                        row.getValues("Username").toString().replaceAll("[\\[\\]]", "");
                if (row.getValues("Name") != null) {
                    user.mFullName =
                            row.getValues("Name").toString().replaceAll("[\\[\\]]", "");
                }
                if (row.getValues("Email") != null) {
                    user.mSipNumber =
                            row.getValues("Email").toString().replaceAll("[\\[\\]]", "");
                }
                if (row.getValues("sip") != null) {
                    user.mSipNumber =
                            row.getValues("sip").toString().replaceAll("[\\[\\]]", "");
                }
                users.add(user);
            }

//            getUsersTrials = 0;
            return users;

        });

    }

    public void setMessageListener() {

        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        Log.d("shit",
                "setMessageListener " + mConnection.toString() + " " + chatManager.toString());
//        if (chatListener == null) {
//            chatListener = (chat, createdLocally) -> {
//                Log.d("shit", "chatCreated");
//                chat.addMessageListener(XmppHelper.this);
//            };
//        }
        try {
            chatManager.removeListener(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        chatManager.addIncomingListener(this);
    }

    public io.reactivex.Observable<Message> sendMessagerx(String mPhoneNumber, String messageString) {
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.setBody(messageString);

            EntityBareJid userJid = JidCreate.entityBareFrom(mPhoneNumber + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat =
                    chatManager.chatWith(userJid);
            
////
//            try {
//                HashMap<OmemoDevice, OmemoFingerprint> fingerprints =
//                        omemoManager.getActiveFingerprints(userJid);
//                for (OmemoDevice d : fingerprints.keySet()) {
//                    Log.d("encrypting", "trust " + OmemoKeyUtil.prettyFingerprint(fingerprints.get(d)));
//                    omemoManager.trustOmemoIdentity(d, fingerprints.get(d));
//                }
//            } catch (Exception e){
//                e.printStackTrace();
//            }


            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);
//            OmemoManager.getInstanceFor(mConnection).buildSessionsWith(userJid);

//            org.jivesoftware.smack.packet.Message encrypted = null;
//            try {
//                encrypted = OmemoManager.getInstanceFor(mConnection).encrypt(userJid, message.toString());
//            }
//            // In case of undecided devices
//            catch (UndecidedOmemoIdentityException e) {
//                e.printStackTrace();
//                Log.d("encrypting", "Undecided Identities: ");
//                for (OmemoDevice device : e.getUntrustedDevices()) {
//                    Log.d("encrypting", device.toString());
//                }
//            }
//            //In case we cannot establish session with some devices
//            catch (CannotEstablishOmemoSessionException e) {
//                e.printStackTrace();
//                encrypted = omemoManager.encryptForExistingSessions(e, message.toString());
//            }

            chat.send(message);
            Message messageForDb = new Message();
            messageForDb.sender = Utils.getPhoneNumber(CozyChatApplication.getContext());
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
            return messageForDb;
        });
    }

    public void saveGcmToken(String token) {
        getSignedInObservable().subscribe(xmppConnection -> {
            try {
                VCard vCard = VCardManager.getInstanceFor(mConnection)
                        .loadVCard();
                Log.d("***", "magic is gong to happen" + new Gson().toJson(vCard));
                vCard.setField("GCMID", token);
                vCard.save(mConnection);
                Log.d("***", "magic happened");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

//    public void sendMessage(Message localMessage) {
//        if (! mConnection.isConnected()) {
//            try {
//                loginOrRegister();
//            } catch (SmackException e) {
//                e.printStackTrace();
//                if (e instanceof SmackException.ConnectionException) {
//                    loginTrials = 0;
//                    Toaster.toast("Network error, try later");
//                    return;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Toaster.toast("An error occurred: " + e.getMessage());
//                return;
//            }
//        }
//        org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
//        message.setBody(localMessage.body);
//
//        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
//        try {
//            Chat chat =
//                    chatManager.createChat(localMessage.receiver + "@" + SERVICE_NAME);
//
//            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);
//
//
//            chat.sendMessage(message);
//            localMessage.messageId = deliveryReceiptId;
//            localMessage.save();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toaster.toast("Error while sending message: " + e.getMessage());
//        }
//
//
//    }

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



    public io.reactivex.Observable<Boolean> sendFileMessagerx(Message mItem, String messageString) {
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.setBody(messageString);

            EntityBareJid userJid = JidCreate.entityBareFrom(mItem.receiver + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat =
                    chatManager.chatWith(userJid);

            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);


            chat.send(message);
            mItem.messageId = deliveryReceiptId;
            mItem.body = messageString;
            mItem.fileKey = messageString.split(Constants.MESSAGE_FILE_INDEX_PREFIX)[0];
            mItem.update();
            return true;
        });


    }

//    public io.reactivex.Observable<Boolean> createUserrx(String login, String email, String password) {
//
//        return getConnectedObservable().subscribeOn(Schedulers.newThread()).map( xmppConnection -> {
//
//            Log.d("xmpp", "create user");
//            AccountManager accountManager = AccountManager.getInstance(mConnection);
//            accountManager.sensitiveOperationOverInsecureConnection(true);
//
//            Map<String, String> attributes = new HashMap<>();
//            attributes.put("email", email);
//            accountManager.createAccount(login, password, attributes);
//            return true;
//        });
//    }

    private io.reactivex.Observable<XMPPConnection> loginrx(){
        if (mConnection == null) {
            init();
        }
        return io.reactivex.Observable.create(emitter -> {
            ConnectionListener listener = new ConnectionListener() {
                @Override
                public void connected(XMPPConnection connection) {
                }

                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {

                    emitter.onNext(connection);
                    connection.removeConnectionListener(this);

//                    try {
//                        SignalOmemoService.acknowledgeLicense();
//                        SignalOmemoService.setup();
//
//                        Log.d("encrypting", OmemoManager.serverSupportsOmemo(mConnection, JidCreate.domainBareFrom(SERVICE_NAME)) + " " );
//
//                        OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(DocumentHelper.createOrOpenOmemoFile());
//                        omemoManager = OmemoManager.getInstanceFor(mConnection);
//                        omemoManager.addOmemoMessageListener(new OmemoMessageListener() {
//                            @Override
//                            public void onOmemoMessageReceived(String decryptedBody, org.jivesoftware.smack.packet.Message encryptedMessage, org.jivesoftware.smack.packet.Message wrappingMessage, OmemoMessageInformation omemoInformation) {
//                                Log.d("omemo-test", "(O) " + encryptedMessage.getFrom() + ": " + decryptedBody);
//                            }
//
//                            @Override
//                            public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, org.jivesoftware.smack.packet.Message message, org.jivesoftware.smack.packet.Message wrappingMessage, OmemoMessageInformation omemoInformation) {
//
//                            }
//                        });
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
                }

                @Override
                public void connectionClosed() {

                }

                @Override
                public void connectionClosedOnError(Exception e) {
                    emitter.onError(e);
                    mConnection.removeConnectionListener(this);
                }

                @Override
                public void reconnectionSuccessful() {

                }

                @Override
                public void reconnectingIn(int seconds) {

                }

                @Override
                public void reconnectionFailed(Exception e) {

                }
            };
            mConnection.addConnectionListener(listener);
            try {
                mConnection.login(mLogin, mLogin, null);
            } catch (Exception e){
                emitter.onError(e);
            }
        });
    }

    @Override
    public void newIncomingMessage(EntityBareJid from, org.jivesoftware.smack.packet.Message message, Chat chat) {
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

        messageForDb.sender = from.getLocalpartOrNull().intern();
        messageForDb.isFromMe = false;
        messageForDb.time = System.currentTimeMillis();
        messageForDb.messageId = message.getStanzaId();
        messageForDb.receiver = Utils.getPhoneNumber(CozyChatApplication.getContext());

        Log.d("***", messageForDb.body + " " + messageForDb.sender + " " + message.getStanzaId());
        if (! TextUtils.isEmpty(messageForDb.body)) {
            messageForDb.save();
            if (! MessageFragment.isActive) {
                Utils.sendNotification(CozyChatApplication.getContext());
            }
            EventBus.getDefault().post(messageForDb);
        }

        LocalBroadcastManager broadcaster =
                LocalBroadcastManager.getInstance(CozyChatApplication.getContext());

        Intent intent = new Intent(MESSAGE_RECEIVED);
        intent.putExtra(MESSAGE_EXTRA, messageForDb.body);
        intent.putExtra(FROM_EXTRA, messageForDb.receiver);
        intent.putExtra(TIME_EXTRA, messageForDb.time);
        broadcaster.sendBroadcast(intent);

    }




//    public io.reactivex.Observable<XMPPConnection> loginrx() {
//        return loginrx(mLogin, mLogin);
//    }

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
