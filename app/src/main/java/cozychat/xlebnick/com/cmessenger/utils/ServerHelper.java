package cozychat.xlebnick.com.cmessenger.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.xdata.Form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cozychat.xlebnick.com.cmessenger.SmackConnection;
import cozychat.xlebnick.com.cmessenger.model.Message;
import cozychat.xlebnick.com.cmessenger.model.User;

/**
 * Created by XlebNick for CMessenger.
 */

public class ServerHelper {


    private static ServerHelper mInstance;
    private String mLogin;
    private AbstractXMPPConnection mConnection;

    public static final String MESSAGE_RECEIVED = "cozychat.net.MESSAGE_RECEIVED";
    public static final String FROM_EXTRA = "cozychat.net.FROM_EXTRA";
    public static final String TIME_EXTRA = "cozychat.net.DATE_EXTRA";
    public static final String MESSAGE_EXTRA = "cozychat.net.MESSAGE_EXTRA";


    private ServerHelper(String login){
        mLogin = login;

        Log.d("shit", login + " login");

        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(login, login)
                .setServiceName("cozychat.net")
                .setHost("78.46.85.86")
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build();
        mConnection = new XMPPTCPConnection(config);

        DeliveryReceiptManager.getInstanceFor(mConnection).addReceiptReceivedListener(new ReceiptReceivedListener() {
            @Override
            public void onReceiptReceived(String fromJid, String toJid, String deliveryReceiptId, Stanza stanza) {
                Log.d("shit", "onReceiptReceived: from: " + fromJid + " to: " + toJid + " deliveryReceiptId: " + deliveryReceiptId + " stanza: " + stanza);
            }
        });


        loginOrRegister();

    }

    public static ServerHelper getInstance(Context context, String login) {
        if (mInstance == null){
            String savedLogin = Utils.getPhoneNumber(context);
            if (savedLogin == null){
                if (login == null){
                    throw new NullPointerException("No saved login was found and argument login is null");
                }

                mInstance = new ServerHelper(login);
            } else {
                mInstance = new ServerHelper(savedLogin);
            }
        }
        return mInstance;
    }

    public void loginOrRegister(){
        try {
            mConnection.connect().login();
        } catch (SmackException| IOException| XMPPException e) {
            e.printStackTrace();
            if (e.getMessage().contains("SASLError using SCRAM-SHA-1: not-authorized")){
                //user is not registered. we'll register him/her

                AccountManager accountManager = AccountManager.getInstance(mConnection);
                try {
                    accountManager.sensitiveOperationOverInsecureConnection(true);
                    accountManager.createAccount(mLogin, mLogin);
                    mConnection.login();
                } catch ( XMPPException | IOException | SmackException e1) {
                    e1.printStackTrace();
                }
            }
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
                return users;
            } else {
                Log.d("***", "No result found");
            }

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setMessageListener(final Context context){
        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        chatManager.addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                chat.addMessageListener(new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                        Message messageForDb = new Message();
                        messageForDb.body = message.getBody();
                        messageForDb.sender = message.getFrom().substring(0, message.getFrom().indexOf("@"));
                        messageForDb.isFromMe = false;
                        messageForDb.time = System.currentTimeMillis();
                        messageForDb.id = message.getStanzaId();
                        Log.d("shit", messageForDb.body +  " " + messageForDb.receiver + " " + message.getStanzaId());
                        if (!TextUtils.isEmpty(messageForDb.body)){
                            messageForDb.save();
                            Utils.sendNotification(context, messageForDb.receiver, messageForDb.body);
                        }


                        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(context);

                        Intent intent = new Intent(MESSAGE_RECEIVED);
                        if(message != null)
                            intent.putExtra(MESSAGE_EXTRA, messageForDb.body);
                            intent.putExtra(FROM_EXTRA, messageForDb.receiver);
                            intent.putExtra(TIME_EXTRA, messageForDb.time);
                        broadcaster.sendBroadcast(intent);


                    }
                });
            }
        });
    }

    public void sendMessage(Context context, String mPhoneNumber, String messageString) {
        if (!mConnection.isConnected()){
            loginOrRegister();
        }
        Message message = new Message();
        message.receiver = mPhoneNumber;
        message.sender = Utils.getPhoneNumber(context);
        message.body = messageString;

        SmackConnection.getInstance(context).sendMessage(message);
//        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
//        try {
//            Chat chat =
//                    chatManager.createChat(mPhoneNumber + "@cozychat.net");
//
//            String deliveryReceiptId = DeliveryReceiptRequest.addTo(message);
//            chat.sendMessage(message);
//
//            Log.d("shit", "sendMessage: deliveryReceiptId for this Message is: " + deliveryReceiptId);
//
//        }catch (SmackException.NotConnectedException e ){
//            loginOrRegister();
//            e.printStackTrace();
//            SmackConnection.getInstance()
//            sendMessage(mPhoneNumber, messageString);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
