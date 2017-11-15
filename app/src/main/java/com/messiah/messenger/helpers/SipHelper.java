package com.messiah.messenger.helpers;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.util.Log;

import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.utils.Utils;

import org.acra.ACRA;
import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.OnRegStateParam;

import java.text.ParseException;

import io.reactivex.Observable;

/**
 * Created by XlebNick for CMessenger.
 */

public class SipHelper {

    private static final String SERVER_DOMAIN = "ec2-35-162-177-84.us-west-2.compute.amazonaws.com";
    private static SipHelper instance = null;
    private SipManager mSipManager = null;
    private SipProfile mSipProfile = null;
    public SipAudioCall call;

    private EpConfig epConfig;
    private MyAccount acc;

    private Endpoint ep;
    private AccountConfig acfg;
    private AuthCredInfo cred;

    private SipHelper() {
        mSipManager = SipManager.newInstance(CozyChatApplication.getContext());


    }

    public static SipHelper getInstance() {
//        if (!(SipManager.isApiSupported(context) && SipManager.isVoipSupported(context))){
//            Toaster.toast("Your device does not support SIP");
//            return new SipHelper();
//        }
        if (instance == null || instance.mSipManager == null)
            instance = new SipHelper();
        return instance;
    }

    public void register() {
//        String sipNumber = Utils.getSipNumber(CozyChatApplication.getContext());
//
//        sipNumber = "6666";
//        if (sipNumber == null || sipNumber.isEmpty())
//            return;
//        String finalSipNumber = sipNumber;
//        new AsyncTask<Void, Void, Void>(){
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                try{
//                    ep = new Endpoint();
//                    ep.libCreate();
//
//                    // Initialize endpoint
//                    epConfig = new EpConfig();
//                    epConfig.getLogConfig().setLevel(5);
//                    ep.libInit( epConfig );
//                    // Create SIP transport. Error handling sample is shown
//                    TransportConfig sipTpConfig = new TransportConfig();
//                    sipTpConfig.setPort(5060);
//                    ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
//                    // Start the library
//                    ep.libStart();
//
//                    acfg = new AccountConfig();
//                    acfg.setIdUri("sip:" + finalSipNumber + "@" + "");
//                    acfg.getRegConfig().setRegistrarUri("sip:" + SERVER_DOMAIN);
//                    cred = new AuthCredInfo("digest", "*", finalSipNumber, 0, "unsecurepassword");
//                    acfg.getSipConfig().getAuthCreds().add( cred );
//                    // Create the account
//                    acc = new MyAccount();
//                    acc.create(acfg);
//                    // Here we don't have anything else to do..
//                    Thread.sleep(10000);
//          /* Explicitly delete the account.
//           * This is to avoid GC to delete the endpoint first before deleting
//           * the account.
//           */
//                    acc.delete();
//
//                    // Explicitly destroy and delete endpoint
//                    ep.libDestroy();
//                    ep.delete();
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
//                return null;
//            }
//        }.execute();

//        try {
//            sipNumber = "6666";
//            mSipProfile = new SipProfile.Builder(sipNumber, SERVER_DOMAIN)
//                    .setPassword("unsecurepassword")
//                    .setPort(5060)
//                    .setSendKeepAlive(true)
//                    .setAutoRegistration(true)
//                    .setProtocol("UDP")
//                    .build();
//
//            Log.d("***", "registering " + mSipProfile.getUriString());
//
//            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                    CozyChatApplication.getContext(), 0,
//                    new Intent("android.SipDemo.INCOMING_CALL"),
//                    Intent.FILL_IN_DATA);
//
//            Log.d("***", CozyChatApplication.getContext().getCacheDir().delete() + "");
//            try {
//                Log.d("***", CozyChatApplication.getContext().getExternalCacheDir().delete() + "");
//            } catch (NullPointerException ignored){}
//            try {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    Log.d("***", CozyChatApplication.getContext().getCodeCacheDir().delete() + "");
//                }
//            } catch (NullPointerException ignored){}
//
//            mSipManager.close(mSipProfile.getUriString());
//            SipRegistrationListener listener = new SipRegistrationListener() {
//
//                public void onRegistering(String localProfileUri) {
//                    Log.d("***", "Registering with SIP Server...");
//                }
//
//                public void onRegistrationDone(String localProfileUri, long expiryTime) {
//                    Log.d("***", "Ready " + mSipProfile.getUriString());
//                }
//
//                public void onRegistrationFailed(String localProfileUri, int errorCode,
//                                                 String errorMessage) {
//
//                    try {
//                        if (errorCode == SipErrorCode.IN_PROGRESS){
//                            Log.d("***", "already exists");
//                            mSipManager.close(mSipProfile.getUriString());
//                            Log.d("***", "over and over");
//                            register();
//                        } else {
//                            mSipManager.close(mSipProfile.getUriString());
//                            mSipManager.open(mSipProfile);
//                            Log.d("***", "Registration failed.  Please check settings." + errorMessage + " " + errorCode);
//                        }
//                    } catch (SipException e) {
//                        Log.d("***", "ahuet'");
//                        e.printStackTrace();
//                    }
//
//
//                }
//            };
//            mSipManager.setRegistrationListener(mSipProfile.getUriString(), listener);
//            mSipManager.open(mSipProfile, pendingIntent, null);
//            mSipManager.register(mSipProfile, 3600, listener);
//
//        } catch (ParseException | SipException e) {
//
//            Log.d("***", "ZHOPA when registering " + mSipProfile.getUriString());
//            e.printStackTrace();
//        }

    }

    public void call(String to, SipAudioCall.Listener listener) {
        to = "sip:" + to + "@" + SERVER_DOMAIN;
        try {

            Log.d("***", "me, " + mSipProfile.getUriString() + ", calls to  " +
                    to);
            call = mSipManager.makeAudioCall(mSipProfile.getUriString(),
                    to,
                    listener,
                    30);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void takeCall(Intent intent, SipAudioCall.Listener listener) {
        try {
            call = mSipManager.takeAudioCall(intent, listener);
            call.answerCall(10);
            call.startAudio();
            call.setSpeakerMode(true);
            if (call.isMuted()) {
                call.toggleMute();
            }
        } catch (SipException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }

    public void endCall() {
        if (call != null){
            try {
                call.endCall();
                call.close();
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }

    public String getCallee(Intent intent) {
        try {
            return mSipManager.getSessionFor(intent).getPeerProfile().getUserName();
        } catch (SipException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Observable registerRx(){
        return Observable.create(e -> {
            String sipNumber = Utils.getSipNumber(CozyChatApplication.getContext());

            if (sipNumber == null || sipNumber.isEmpty()){
                e.onError(new NullPointerException());
                return;
            }
            try {
                mSipProfile = new SipProfile.Builder(sipNumber, SERVER_DOMAIN)
                        .setPassword("unsecurepassword")
                        .setPort(5060)
                        .setSendKeepAlive(true)
                        .setAutoRegistration(true)
                        .setProtocol("UDP")
                        .build();

                Log.d("***", "registering " + mSipProfile.getUriString());

                final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        CozyChatApplication.getContext(), 0,
                        new Intent("android.SipDemo.INCOMING_CALL"),
                        Intent.FILL_IN_DATA);

                mSipManager.close(mSipProfile.getUriString());

                SipRegistrationListener listener = new SipRegistrationListener() {

                    public void onRegistering(String localProfileUri) {
                        Log.d("***", "Registering with SIP Server...");
                    }

                    public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        Log.d("***", "Ready " + mSipProfile.getUriString());
                        e.onNext(localProfileUri);
                    }

                    public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                     String errorMessage) {
                        e.onNext(new RuntimeException("errorMessage"));
//
//                        try {
//                            if (errorCode == SipErrorCode.IN_PROGRESS){
//                                Log.d("***", "already exists");
//                                mSipManager.close(mSipProfile.getUriString());
//                                Log.d("***", "over and over");
//                                register();
//                            } else {
//                                mSipManager.close(mSipProfile.getUriString());
//                                mSipManager.open(mSipProfile);
//                                Log.d("***", "Registration failed.  Please check settings." + errorMessage + " " + errorCode);
//                            }
//                        } catch (SipException e) {
//                            Log.d("***", "ahuet'");
//                            e.printStackTrace();
//                        }


                    }
                };
                mSipManager.setRegistrationListener(mSipProfile.getUriString(), listener);
                mSipManager.open(mSipProfile, pendingIntent, null);
                mSipManager.register(mSipProfile, 3600, listener);

            } catch (ParseException | SipException ex) {

                Log.d("***", "ZHOPA when registering " + mSipProfile.getUriString());
                ex.printStackTrace();
                e.onError(ex);
            }
        });

    }

    class MyAccount extends Account {
        @Override
        public void onRegState(OnRegStateParam prm) {
            System.out.println("*** On registration state: " + prm.getCode() + prm.getReason());
        }
    }

}
