package net.xlebnick.geechat;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.util.Log;

import org.acra.ACRA;

import java.text.ParseException;

/**
 * Created by XlebNick for CMessenger.
 */

public class SipHelper {

    private final Context mContext;
    public SipManager mSipManager = null;
    public static SipHelper instance = null;
    public SipProfile mSipProfile = null;
    public SipAudioCall call;

    private SipHelper(Context context) {

        if(mSipManager == null) {
            mSipManager = SipManager.newInstance(context);
        }
        mContext = context;

    }

    public void register(String sipNumber){

        SipProfile.Builder builder;
        try {
            builder = new SipProfile.Builder(sipNumber, "78.46.85.86");
            builder.setPassword("unsecurepassword");
            builder.setPort(5060);
            mSipProfile = builder.build();
            Log.d("***", sipNumber);

            Intent intent = new Intent();
            intent.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, Intent.FILL_IN_DATA);

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
                    Log.d("***", "Registration failed.  Please check settings." + errorMessage + " " + errorCode);
                }
            });
        } catch (ParseException | SipException e) {
            e.printStackTrace();
        }

    }

    public static SipHelper getInstance(Context context) {
        if (instance == null)
            instance = new SipHelper(context);
        return instance;
    }


    public void call(String to, SipAudioCall.Listener listener) {
        try {

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
            if(call.isMuted()) {
                call.toggleMute();
            }
        } catch (SipException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }
}
