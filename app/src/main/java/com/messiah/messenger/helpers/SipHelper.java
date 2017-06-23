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

import org.acra.ACRA;

import java.text.ParseException;

import xdroid.toaster.Toaster;

/**
 * Created by XlebNick for CMessenger.
 */

public class SipHelper {

    public static SipHelper instance = null;
    private final Context mContext;
    public SipManager mSipManager = null;
    public SipProfile mSipProfile = null;
    public SipAudioCall call;

    private SipHelper(Context context) {

        if (mSipManager == null) {
            mSipManager = SipManager.newInstance(context);
        }
        mContext = context;

    }

    public SipHelper() {
        mContext = null;
    }

    public static SipHelper getInstance(Context context) {
        if (!(SipManager.isApiSupported(context) && SipManager.isVoipSupported(context))){
            Toaster.toast("Your device does not support SIP");
            return new SipHelper();
        }
        if (instance == null)
            instance = new SipHelper(context);
        return instance;
    }

    public void register(final String sipNumber) {

        Log.d("***", "sdcsdcsdcsdcsdcsdcsd");
        SipProfile.Builder builder;
        try {
            builder = new SipProfile.Builder(sipNumber, "ec2-34-208-141-31.us-west-2.compute.amazonaws.com");
            builder.setPassword("unsecurepassword");
            builder.setOutboundProxy("ec2-34-208-141-31.us-west-2.compute.amazonaws.com");
            builder.setPort(5060);
            mSipProfile = builder.build();

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, Intent.FILL_IN_DATA);

            Log.d("***", " " + SipManager.isVoipSupported(mContext) + " " + SipManager.isApiSupported(mContext));

            mSipManager.open(mSipProfile, pendingIntent, null);
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
                                register(sipNumber);
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
        } catch (ParseException | SipException e) {
            e.printStackTrace();
        }

    }

    public void call(String to, SipAudioCall.Listener listener) {
        try {

            Log.d("***", mSipProfile.getUriString() + " here " +
                    to);
            call = mSipManager.makeAudioCall(mSipProfile.getUriString(),
                    to,
                    listener,
                    0);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void takeCall(Intent intent, SipAudioCall.Listener listener) {
        try {
            call = mSipManager.takeAudioCall(intent, listener);
            call.answerCall(0);
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
}
