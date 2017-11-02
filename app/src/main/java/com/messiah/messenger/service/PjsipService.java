package com.messiah.messenger.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.messiah.messenger.Constants;
import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.activity.DialActivity;
import com.messiah.messenger.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.app.MyAccount;
import org.pjsip.pjsua2.app.MyApp;
import org.pjsip.pjsua2.app.MyAppObserver;
import org.pjsip.pjsua2.app.MyBuddy;
import org.pjsip.pjsua2.app.MyCall;
import org.pjsip.pjsua2.pjsip_status_code;
/**
 * Created by XlebNick for CMessenger.
 */

public class PjsipService extends Service implements MyAppObserver {

    public MyApp app = null;
    public static MyCall currentCall = null;
    public static MyAccount account = null;
    public AccountConfig accCfg = null;
//    private NotificationManager notificationManager;

//    ArrayList<Map<String, String>> buddyList;

    @Override
    public void onCreate() {
        super.onCreate();
//        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        init();
    }

    public void init() {
        Log.d("pjsip", "initiating...");

        if (app == null) {
            app = new MyApp();
            // Wait for GDB to init, for native debugging only

            app.init(this, CozyChatApplication.getContext().getFilesDir().getAbsolutePath());
        }

        if (app.accList.size() == 0) {

            String username = Utils.getSipNumber(this);
            accCfg = new AccountConfig();
            accCfg.setIdUri("sip:" + username + "@" + Constants.SIP_SERVER_ADDRESS);
            accCfg.getRegConfig().setRegistrarUri("sip:" + Constants.SIP_SERVER_HOSTNAME);
            AuthCredInfoVector creds = accCfg.getSipConfig().
                    getAuthCreds();
            creds.clear();
            if (username != null &&username.length() != 0) {
                creds.add(new AuthCredInfo("Digest", "*", username, 0,
                        "unsecurepassword"));
            }
            accCfg.getNatConfig().setIceEnabled(true);
            account = app.addAcc(accCfg);
        } else {
            account = app.accList.get(0);
            accCfg = account.cfg;
        }
//
//        buddyList = new ArrayList<>();
//        for (int i = 0; i < account.buddyList.size(); i++) {
//            buddyList.add(putData(account.buddyList.get(i).cfg.getUri(),
//                    account.buddyList.get(i).getStatusText()));
//        }
    }

//    private HashMap<String, String> putData(String uri, String status) {
//        HashMap<String, String> item = new HashMap<>();
//        item.put("uri", uri);
//        item.put("status", status);
//        return item;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void notifyRegState(pjsip_status_code code, String reason, int expiration) {
        Log.d("pjsip", code + " " + reason + "; expires in " + expiration);
    }

    @Override
    public void notifyIncomingCall(MyCall call) {
        Log.d("pjsip", "notifyIncomingCall");
        if (currentCall != null){
            CallOpParam param = new CallOpParam();
            param.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
            try {
                call.hangup(param);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                call.delete();
            } catch (Exception e){
                e.printStackTrace();
            }
            return;
        }

        currentCall = call;

        Intent intent = new Intent(this, DialActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }

    @Override
    public void notifyCallState(MyCall call) {

        Log.d("pjsip", "notifyCallState");
        try {
            if (call != null) {
                EventBus.getDefault().post(new CallStateEvent(call.getInfo()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyCallMediaState(MyCall call) {

        Log.d("pjsip", "notifyCallMediaState");
        try {
            EventBus.getDefault().post(new CallMediaStateEvent(call.getInfo()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyBuddyState(MyBuddy buddy) {

    }

//    private void showNotification(String text, @DrawableRes int iconId, Class contentIntent) {
//        // In this sample, we'll use the same text for the ticker and the expanded notification
//
//        // The PendingIntent to launch our activity if the user selects this notification
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, DialActivity.class), 0);
//
//        // Set the info for the views that show in the notification panel.
//        Notification notification = new Notification.Builder(this)
//                .setSmallIcon(iconId)  // the status icon
//                .setTicker(text)  // the status text
//                .setWhen(System.currentTimeMillis())  // the time stamp
//                .setContentTitle(getString(R.string.missing_call))  // the label of the entry
//                .setContentText(text)  // the contents of the entry
//                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
//                .build();
//
//        // Send the notification.
//        notificationManager.notify(NOTIFICATION, notification);
//    }

    public static class CallStateEvent {

        public CallInfo callInfo;

        CallStateEvent(CallInfo callInfo){
            this.callInfo = callInfo;
        }
    }

    public static class CallMediaStateEvent {

        public CallInfo callInfo;

        CallMediaStateEvent(CallInfo callInfo){
            this.callInfo = callInfo;
        }
    }

    public static void call(String sipNumber){
        currentCall = new MyCall(account, -1);
        CallOpParam param = new CallOpParam(true);
        try {
            currentCall.makeCall("sip:" + sipNumber + "@" + Constants.SIP_SERVER_ADDRESS, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
