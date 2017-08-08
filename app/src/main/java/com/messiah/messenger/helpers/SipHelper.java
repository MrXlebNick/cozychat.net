package com.messiah.messenger.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipErrorCode;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.util.Log;
import android.widget.Toast;

import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.utils.Utils;

import org.acra.ACRA;

import java.text.ParseException;

import xdroid.toaster.Toaster;

/**
 * Created by XlebNick for CMessenger.
 */

public class SipHelper {

    private static final String SERVER_DOMAIN = "ec2-35-165-67-249.us-west-2.compute.amazonaws.com";
    private static SipHelper instance = null;
    private SipManager mSipManager = null;
    private SipProfile mSipProfile = null;
    public SipAudioCall call;

    private SipHelper() {
        mSipManager = SipManager.newInstance(CozyChatApplication.getContext());

    }

    public static SipHelper getInstance() {
//        if (!(SipManager.isApiSupported(context) && SipManager.isVoipSupported(context))){
//            Toaster.toast("Your device does not support SIP");
//            return new SipHelper();
//        }
        if (instance == null)
            instance = new SipHelper();
        return instance;
    }

    public void register() {
        String sipNumber = Utils.getSipNumber(CozyChatApplication.getContext());

        Log.d("***", "sdcsdcsdcsdcsdcsdcsd");
        SipProfile.Builder builder;
        try {
            builder = new SipProfile.Builder(sipNumber, SERVER_DOMAIN);
            builder.setPassword("unsecurepassword");
            builder.setOutboundProxy(SERVER_DOMAIN);
            builder.setPort(5060);
            mSipProfile = builder.build();

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(CozyChatApplication.getContext(), 0, intent, Intent.FILL_IN_DATA);

            Log.d("***", " " + SipManager.isVoipSupported(CozyChatApplication.getContext()) + " " + SipManager.isApiSupported(CozyChatApplication.getContext()));

            mSipManager.setRegistrationListener(mSipProfile.getUriString(), new SipRegistrationListener() {

                public void onRegistering(String localProfileUri) {
                    Log.d("***", "Registering with SIP Server...");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    Log.d("***", "Ready " + mSipProfile.getUriString());
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {

                        try {
                            if (errorCode == SipErrorCode.IN_PROGRESS){
                                Log.d("***", "already exists");
                                mSipManager.close(mSipProfile.getUriString());
                                Log.d("***", "over and over");
                                register();
                            } else {
                                mSipManager.close(mSipProfile.getUriString());
                                mSipManager.open(mSipProfile);
                                Log.d("***", "Registration failed.  Please check settings." + errorMessage + " " + errorCode);
                            }
                        } catch (SipException e) {
                            Log.d("***", "ahuet'");
                            e.printStackTrace();
                        }


                }
            });
            mSipManager.open(mSipProfile, pendingIntent, null);
            mSipManager.register(mSipProfile, 3600, null);
//            mSipManager.unregister(mSipProfile, null);
        } catch (ParseException | SipException e) {
            e.printStackTrace();
        }

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
        if (call != null && call.isInCall()){
            try {
                call.endCall();
                call.close();
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }
}
