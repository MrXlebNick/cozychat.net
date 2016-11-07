package cozychat.xlebnick.com.cmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.util.Log;

import cozychat.xlebnick.com.cmessenger.activity.MainActivity;

/**
 * Created by XlebNick for CMessenger.
 */public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        SipAudioCall incomingCall = null;
        try {
            Log.d("***", "onReceive");
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        Log.d("***", "onRinging");
                        call.answerCall(30);
                        call.startAudio();
                        call.setSpeakerMode(true);
                        if(call.isMuted()) {
                            call.toggleMute();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            MainActivity wtActivity = (MainActivity) context;
            incomingCall = wtActivity.mSipManager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if(incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }
            wtActivity.call = incomingCall;
        } catch (Exception e) {
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }
}
