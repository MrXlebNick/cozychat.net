package com.messiah.messenger.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.fragment.MessageFragment;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.model.SecretDialogData;
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
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
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
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

//import org.jivesoftware.smackx.omemo.OmemoManager;

/**
 * Created by XlebNick for CMessenger.
 */

public class XmppHelper implements IncomingChatMessageListener, StanzaListener {

    private static final String MESSAGE_RECEIVED = "cozychat.net.MESSAGE_RECEIVED";
    private static final String FROM_EXTRA = "cozychat.net.FROM_EXTRA";
    private static final String TIME_EXTRA = "cozychat.net.DATE_EXTRA";
    private static final String MESSAGE_EXTRA = "cozychat.net.MESSAGE_EXTRA";
    private static final String SERVICE_NAME = "ec2-35-162-177-84.us-west-2.compute.amazonaws.com";
    private static XmppHelper mInstance;
    private String mLogin;
    private AbstractXMPPConnection mConnection;
//    private ChatManagerListener chatListener;
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

        ProviderManager.addExtensionProvider("DH-Nikita-Inv",
                "dh-nikita-inv",
                new StandardExtensionElementProvider());
        ProviderManager.addExtensionProvider("DH-Nikita-Acc",
                "dh-nikita-acc",
                new StandardExtensionElementProvider());
        ProviderManager.addExtensionProvider("DH-Nikita-Message-Err",
                "dh-nikita-message-error",
                new StandardExtensionElementProvider());
        ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT,
                DeliveryReceipt.NAMESPACE,
                new DeliveryReceipt.Provider());
        ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT,
                DeliveryReceipt.NAMESPACE,
                new DeliveryReceipt.Provider());
        ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT,
                DeliveryReceipt.NAMESPACE,
                new DeliveryReceipt.Provider());
        ProviderManager.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
                new DeliveryReceiptRequest().getNamespace(),
                new DeliveryReceiptRequest.Provider());

        XMPPTCPConnectionConfiguration config = null;
        try {
            config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(mLogin, mLogin)
                    .setXmppDomain(SERVICE_NAME)
                    .setHost(SERVICE_NAME)
                    .setConnectTimeout(10000)
                    .setPort(5222)
                    .setSendPresence(true)
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
        DeliveryReceiptManager.setDefaultAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
        DeliveryReceiptManager.getInstanceFor(mConnection).autoAddDeliveryReceiptRequests();

        DeliveryReceiptManager.getInstanceFor(mConnection)
                .addReceiptReceivedListener((fromJid, toJid, deliveryReceiptId, stanza) -> {
                    Stanza received = new org.jivesoftware.smack.packet.Message();
                    received.addExtension(new DeliveryReceipt(deliveryReceiptId));
                    received.setTo(fromJid);
                    try {
                        mConnection.sendStanza(received);
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(
                            "***",
                            "onReceiptReceived: from: " + fromJid + " to: " + toJid +
                                    " deliveryReceiptId: " + deliveryReceiptId + " stanza: " + stanza);
                });

        mConnection.addAsyncStanzaListener(this, null);
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

    private io.reactivex.Observable<XMPPConnection> connectrx(){
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
                    e.printStackTrace();
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
                if (e.getMessage().contains("Client is already connected")) {
                    mConnection.removeConnectionListener(listener);
                    emitter.onNext(mConnection);
                    emitter.onComplete();
                } else {
                    emitter.onError(e);
                }
            }
        });
    }

    
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
        if (!mConnection.isAuthenticated() || mConnection.isAnonymous()){
            observable = observable.flatMap(o -> loginrx());
        }
        observable = observable.onErrorResumeNext(throwable -> throwable.getMessage().contains("not-authorized") ?
                getConnectedObservable().flatMap(xmppConnection -> registerrx())
                    .flatMap(xmppConnection -> getConnectedObservable()
                        .flatMap(xmppConnection2 -> loginrx())) : Observable.error(throwable));
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

        FileIOApi retrofit = new Retrofit.Builder().baseUrl("http://ec2-35-162-177-84.us-west-2.compute.amazonaws.com:8080")
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

            FileIOApi retrofit = new Retrofit.Builder().baseUrl("http://ec2-35-162-177-84.us-west-2.compute.amazonaws.com:8080")
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
                if (!(entry.getKey().equals("niknam") && entry.getValue().equals("NickName"))){
                    vCard.setField(entry.getKey(), entry.getValue());
                    AccountManager am= AccountManager.getInstance(mConnection);
                    Set<String> i=am.getAccountAttributes();
                    HashMap<String, String> map = new HashMap();
                    for (String name  : i){
                        map.put(name,am.getAccountAttribute(name));
                    }
                    map.put("name",entry.getValue());
// create a registration packet
                    Registration reg=new Registration(map);
                    reg.setType(IQ.Type.set); // we''re setting the attributes
                    reg.setFrom(mConnection.getUser()); // set the from address to be from this user
                    reg.setTo(JidCreate.from(mConnection.getHost()));
                    mConnection.sendStanza(reg); // send the packet
                }

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

    public io.reactivex.Observable<List<User>> searchUsersrx(String s) {
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {
            boolean isPhone = Patterns.PHONE.matcher(s).matches();
            UserSearchManager manager = new UserSearchManager(mConnection);
            Form searchForm;

            DomainBareJid searchJid = JidCreate.domainBareFrom("search." + mConnection.getServiceName());

            searchForm = manager.getSearchForm(searchJid);
            Form answerForm = searchForm.createAnswerForm();

            UserSearch userSearch = new UserSearch();
            if (isPhone){

                answerForm.setAnswer("Username", true);
                answerForm.setAnswer("search", "*" + s + "*" );
            } else {
                answerForm.setAnswer("Name", true);
                answerForm.setAnswer("search", "*" + s + "*" );
            }

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
        Log.d("shit", "setMessageListener " + mConnection.toString() + " " + chatManager.toString());
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
        return sendMessagerx(mPhoneNumber, messageString, null);
    }

    public io.reactivex.Observable<Message> sendMessagerx(String mPhoneNumber, String messageString,
                                                          String dialogId) {
        return getSignedInObservable().subscribeOn(Schedulers.newThread()).map(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            String messageBody = messageString;
            if (!TextUtils.isEmpty(dialogId)) {
                SecretDialogData secretDialogData = SecretDialogData
                        .find(SecretDialogData.class, "dialog_id = \"" + dialogId+ "\" AND opponent_number = \"" + mPhoneNumber + "\"")
                        .get(0);
                if (! secretDialogData.isComplete || secretDialogData.secret == null || secretDialogData.secret.length ==0 ) {
                    throw new IndexOutOfBoundsException("Secret is absent");
                }
                messageBody = "!!--ENCRYPTED" + dialogId + Utils.encode(messageBody, secretDialogData.secret);
            }
            message.setBody(URLEncoder.encode(messageBody, "utf-8"));

            EntityBareJid userJid = JidCreate.entityBareFrom(mPhoneNumber + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(userJid);
//
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
//            message = DeliveryReceiptManager.receiptMessageFor(message);
//            OmemoManager.getInstanceFor(mConnection).buildSessionsWith(userJid);
//
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

            Log.d("shit", new Gson().toJson(message));
            chat.send(message);
            Message messageForDb = new Message();
            messageForDb.sender = Utils.getPhoneNumber(CozyChatApplication.getContext());
            messageForDb.receiver = mPhoneNumber;
            messageForDb.body = messageString;
            messageForDb.time = System.currentTimeMillis();
            messageForDb.isFromMe = true;
            messageForDb.messageId = deliveryReceiptId;
            messageForDb.save();
            return messageForDb;
        });
    }

    public void saveGcmToken(String token) {
        getSignedInObservable().subscribeOn(Schedulers.newThread()).subscribe(xmppConnection -> {
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
        },
                Throwable::printStackTrace);
    }

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

    public io.reactivex.Observable<XMPPConnection> loginrx(){
        if (mConnection == null) {
            init();
        }
        return io.reactivex.Observable.create(emitter -> {
            Log.d("xmpp", "logging in");
            ConnectionListener listener = new ConnectionListener() {
                @Override
                public void connected(XMPPConnection connection) {
                }

                @Override
                public void authenticated(XMPPConnection connection, boolean resumed) {

                    emitter.onNext(connection);
                    connection.removeConnectionListener(this);
                    emitter.onComplete();

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
                    mConnection.removeConnectionListener(this);
                    emitter.onError(e);
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
                if (e.getMessage().contains("Client is already logged in")){

                    mConnection.removeConnectionListener(listener);
                    emitter.onNext(mConnection);
                    emitter.onComplete();
                } else {
                    mConnection.removeConnectionListener(listener);
                    emitter.onError(e);
                }
            }
        });
    }

    private io.reactivex.Observable<XMPPConnection> registerrx() {


        return io.reactivex.Observable.create(emitter -> {

            Log.d("xmpp", "creating");
            AccountManager accountManager = AccountManager.getInstance(mConnection);
            try {
                accountManager.sensitiveOperationOverInsecureConnection(true);
                accountManager.createAccount(Localpart.from(mLogin), mLogin);
                mConnection.disconnect();
                emitter.onNext(mConnection);
                emitter.onComplete();

            } catch (XMPPException | IOException | SmackException e1) {
                emitter.onError(e1);
            }
        });

    }

    @Override
    public void newIncomingMessage(EntityBareJid from, org.jivesoftware.smack.packet.Message message, Chat chat) {

        Log.d("***", "newIncomingMessage " );
        StandardExtensionElement dhInv = (StandardExtensionElement) message.getExtension("dh-nikita-inv");

        if (dhInv != null){

            Log.d("***", "Invitation received ");
            String aParameter = dhInv.getAttributeValue("aParameter");
            String pParameter = dhInv.getAttributeValue("pParameter");
            String gParameter = dhInv.getAttributeValue("gParameter");
            String dialogId = dhInv.getAttributeValue("dialogId");
            if (TextUtils.isEmpty(aParameter)){
                Log.e("encrypt", "aParameter is empty");
                return;
            }
            if (TextUtils.isEmpty(pParameter)){
                Log.e("encrypt", "pParameter is empty");
                return;
            }
            if (TextUtils.isEmpty(gParameter)){
                Log.e("encrypt", "gParameter is empty");
                return;
            }
            if (TextUtils.isEmpty(dialogId)){
                Log.e("encrypt", "dialogId is empty");
                return;
            }
            try {
                KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH", "BC");
                DHParameterSpec dhParams = new DHParameterSpec(new BigInteger(pParameter), new BigInteger(gParameter));
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "BC");
                keyGen.initialize(dhParams, new SecureRandom());
                KeyPair bPair = keyGen.generateKeyPair();
                aKeyAgree.init(bPair.getPrivate());

                KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
                X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(new BigInteger(aParameter,16).toByteArray());
                PublicKey bobDHPub = clientKeyFac.generatePublic(x509KeySpec);
                aKeyAgree.doPhase(bobDHPub, true);

                SecretDialogData secretDialogData = new SecretDialogData();
                secretDialogData.opponentNumber = from.getLocalpart().toString();
//                secretDialogData.setPrivateKey();
                secretDialogData.dialogId = dialogId;
                secretDialogData.isComplete = true;
                secretDialogData.secret = aKeyAgree.generateSecret();
                secretDialogData.save();

                sendSecretDialogAcception(bPair.getPublic(), dialogId, secretDialogData.opponentNumber);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }


        StandardExtensionElement dhAcc =  message.getExtension("DH-Nikita-Acc", "dh-nikita-acc");

        if (dhAcc != null){


            Log.d("***", "Second phase received");

            String bParameter = dhAcc.getAttributeValue("bParameter");
            String dialogId = dhAcc.getAttributeValue("dialogId");

            SecretDialogData secretDialogData;
            try {
                secretDialogData = SecretDialogData.find(SecretDialogData.class,
                        " dialog_id = \"" + dialogId + "\" AND opponent_number = \"" + from.getLocalpart().toString() + "\"").get(0);
            } catch (IndexOutOfBoundsException e){
                e.printStackTrace();
                sendSecretAcceptionError(from.getLocalpart().toString(), dialogId, "No such dialog");
                return;
            }

            try {

                KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH", "BC");
                aKeyAgree.init(secretDialogData.getPrivateKey());

                KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
                X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(new BigInteger(bParameter,16).toByteArray());
                PublicKey bobDHPub = clientKeyFac.generatePublic(x509KeySpec);
                aKeyAgree.doPhase(bobDHPub, true);

                secretDialogData.secret = aKeyAgree.generateSecret();
                secretDialogData.isComplete = true;
                secretDialogData.update();

                EventBus.getDefault().post(secretDialogData);
            } catch (Exception e) {

                sendSecretAcceptionError(from.getLocalpart().toString(), dialogId, "Cannot create key");
                e.printStackTrace();
                return;
            }
            return;
        }

        StandardExtensionElement dhEncMessageError =  message.getExtension("DH-Nikita-Message-Err", "dh-nikita-message-err");

        if (dhEncMessageError != null){

            Log.d("***", "error");
            String stanzaId = dhEncMessageError.getAttributeValue("stanzaId");
            String reason = dhEncMessageError.getAttributeValue("reason");

            try {
                Message erroredMessage = Message.find(Message.class, "receiver = ?, message_id = ?",
                        from.getLocalpart().toString(), stanzaId).get(0);
                erroredMessage.error = reason;
                erroredMessage.status = Message.Status.ERROR;
                erroredMessage.update();
                EventBus.getDefault().post(erroredMessage);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return;
        }

        String body;
        Message messageForDb = new Message();
        try {
            body = URLDecoder.decode(message.getBody(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }

        if (body.contains("!!--ENCRYPTED")) {
            String dialogId = body.substring(13, 45);
            String opponent = from.getLocalpart().toString();
            SecretDialogData secretDialogData;
            try {
                secretDialogData = SecretDialogData.find(SecretDialogData.class,
                        "dialog_id = \""+dialogId+"\" AND opponent_number = \""+opponent+"\"")
                        .get(0);
            } catch (IndexOutOfBoundsException e){
                e.printStackTrace();
                sendEncryptedMessageError(opponent, message.getStanzaId(), "No established connection");
                return;
            }

            if (!secretDialogData.isComplete || secretDialogData.secret == null || secretDialogData.secret.length == 0) {
                sendEncryptedMessageError(opponent, message.getStanzaId(), "No established connection");
                return;
            }

            body = Utils.decode(body.substring(45), secretDialogData.secret);
            messageForDb.dialogId = dialogId;
        }

        if (body.contains(MESSAGE_FILE_INDEX_PREFIX)) {
            String[] parts = body.split(Pattern.quote(MESSAGE_FILE_INDEX_PREFIX));
            messageForDb.body = parts[0];
            messageForDb.fileName = parts[1];
            messageForDb.type = Constants.MESSAGE_TYPE_FILE;

        } else {
            messageForDb.type = Constants.MESSAGE_TYPE_TEXT;
            messageForDb.body = body;
        }

        messageForDb.sender = message.getFrom().getLocalpartOrNull().intern();
        messageForDb.isFromMe = false;
        messageForDb.time = System.currentTimeMillis();
        messageForDb.messageId = message.getStanzaId();
        messageForDb.receiver = Utils.getPhoneNumber(CozyChatApplication.getContext());



        Log.d("***", new Gson().toJson(messageForDb));
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

    private void sendEncryptedMessageError(String to, String stanzaId, String reason) {
        getSignedInObservable().subscribeOn(Schedulers.newThread()).subscribe(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.addExtension(StandardExtensionElement.builder("DH-Nikita-Message-Err", "dh-nikita-message-err")
                    .addAttribute("stanzaId", stanzaId)
                    .addAttribute("reason", reason)
                    .build());

            EntityBareJid userJid = JidCreate.entityBareFrom(to + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(userJid);

            Log.d("shit", new Gson().toJson(message));
            chat.send(message);



        });
    }

    private void sendSecretAcceptionError(String to, String dialogId, String reason) {
        getSignedInObservable().subscribeOn(Schedulers.newThread()).subscribe(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.addExtension(StandardExtensionElement.builder("DH-Nikita-Acc-Err", "dh-nikita-acc-err")
                    .addAttribute("reason", reason)
                    .addAttribute("dialogId", dialogId)
                    .build());
            message.addBody("EN", "DIFFIE-HELLMAN");

            EntityBareJid userJid = JidCreate.entityBareFrom(to + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(userJid);

            Log.d("shit", new Gson().toJson(message));
            chat.send(message);



        });
    }

    private void sendSecretDialogAcception(Key b, String dialogId, String to) {
        getSignedInObservable().subscribeOn(Schedulers.newThread()).subscribe(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.addExtension(StandardExtensionElement.builder("DH-Nikita-Acc", "dh-nikita-acc")
                    .addAttribute("bParameter", Utils.bytesToHex(b.getEncoded()))
                    .addAttribute("dialogId", dialogId)
                    .build());

            message.addBody("EN", "DIFFIE-HELLMAN");

            EntityBareJid userJid = JidCreate.entityBareFrom(to + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(userJid);

            Log.d("shit", new Gson().toJson(message));
            chat.send(message);



        });
    }

    public void sendSecretDialogInvitation(String to, BigInteger p, BigInteger g, Key a, String dialogId) {
        getSignedInObservable().subscribeOn(Schedulers.newThread()).subscribe(xmppConnection -> {
            org.jivesoftware.smack.packet.Message message = new org.jivesoftware.smack.packet.Message();
            message.addExtension(StandardExtensionElement.builder("DH-Nikita-Inv", "dh-nikita-inv")
                    .addAttribute("pParameter", p.toString())
                    .addAttribute("gParameter", g.toString())

                    .addAttribute("aParameter", Utils.bytesToHex(a.getEncoded()))
                    .addAttribute("dialogId", dialogId)
                    .build());

            KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH", "BC");
            DHParameterSpec dhParams = new DHParameterSpec(p, g);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "BC");
            keyGen.initialize(dhParams, new SecureRandom());
            KeyPair bPair = keyGen.generateKeyPair();
            aKeyAgree.init(bPair.getPrivate());
            aKeyAgree.doPhase(a, true);

            Log.d("***", a.getClass().getName());

            message.addBody("EN", "DIFFIE-HELLMAN");

            EntityBareJid userJid = JidCreate.entityBareFrom(to + "@" + SERVICE_NAME);
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(userJid);

            Log.d("shit", new Gson().toJson(message));
            chat.send(message);



        });
    }

    @Override
    public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
        Log.d("***", "process Stanza");
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
