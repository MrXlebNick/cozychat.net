package com.messiah.messenger.activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.service.PjsipService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;
import org.pjsip.pjsua2.pjsip_status_code;

public class DialActivity extends AppCompatActivity {
    public static final String IS_INCOMING_CALL = "isIncomingCall";
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;
    private UpdateTextViewThread refreshCounterThread;

    private TextView nameView;
    private TextView phoneView;
    private TextView clockView;
    private TextView statusView;
    private ImageView avatarView;
    private Button hangupButton;
    private Button takeButton;
    private Button declineButton;
    private View parent;

    private long seconds;
    private MediaPlayer thePlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {}

//        final SipAudioCall.Listener listener = new SipAudioCall.Listener() {
//
//            @Override
//            public void onReadyToCall(SipAudioCall call) {
//                Log.d("***", "onReadyToCall");
//                super.onReadyToCall(call);
//            }
//
//            @Override
//            public void onCalling(SipAudioCall call) {
//                Log.d("***", "onCalling");
//                super.onCalling(call);
//            }
//
//            @Override
//            public void onRinging(SipAudioCall call, SipProfile caller) {
//                Log.d("***", "onRinging");
//                super.onRinging(call, caller);
//            }
//
//            @Override
//            public void onRingingBack(SipAudioCall call) {
//                Log.d("***", "onRingingBack");
////                super.onRingingBack(call);
//            }
//
//            @Override
//            public void onCallEstablished(SipAudioCall call) {
//
//                super.onCallEstablished(call);
//                Log.d("***", "onCallEstablished " + call.isInCall() + " " + call.getState());
//
//                call.startAudio();
//                Log.d("***", "onCallEstablished1");
//                call.setSpeakerMode(true);
//                Log.d("***", "onCallEstablished2");
//                if (call.isMuted())
//                    call.toggleMute();
//                Log.d("***", "onCallEstablished3");
//                showCall();
//                Log.d("***", "onCallEstablished4");
//
//            }
//
//            @Override
//            public void onCallEnded(SipAudioCall call) {
//                Log.d("***", "onCallEnded");
//                endCall();
//                super.onCallEnded(call);
//
//            }
//
//            @Override
//            public void onCallBusy(SipAudioCall call) {
//                Log.d("***", "onCallBusy");
//                endCall("Busy");
//            }
//
//            @Override
//            public void onCallHeld(SipAudioCall call) {
//                Log.d("***", "onCallHeld");
//                super.onCallHeld(call);
//            }
//
//            @Override
//            public void onError(SipAudioCall call, int errorCode, final String errorMessage) {
//                Log.d("***", "onError " + errorMessage + " " + errorCode);
////                DialActivity.this.runOnUiThread(new Runnable() {
////                    @Override
////                    public void run() {
////                        Toast.makeText(DialActivity.this, "Error has occured, reason: " + errorMessage, Toast.LENGTH_LONG).show();
////                    }
////                });
//                endCall("Error: message");
//                super.onError(call, errorCode, errorMessage);
//            }
//
//            @Override
//            public void onChanged(SipAudioCall call) {
//                Log.d("***", "onChanged");
//                super.onChanged(call);
//            }
//
//        };


        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        setContentView(R.layout.activity_dial);

        nameView = (TextView) findViewById(R.id.text_name);
        statusView = (TextView) findViewById(R.id.text_status);
        phoneView = (TextView) findViewById(R.id.text_phone);
        clockView = (TextView) findViewById(R.id.text_clock);
        avatarView = (ImageView) findViewById(R.id.avatar);
        hangupButton = (Button) findViewById(R.id.btn_hangup);
        takeButton = (Button) findViewById(R.id.btn_take);
        declineButton = (Button) findViewById(R.id.btn_decline);

        hangupButton.setOnClickListener(v -> endCall());
        takeButton.setOnClickListener(v -> {
//            if (PjsipService.currentCall != null){
//                CallOpParam param = new CallOpParam();
//                param.setStatusCode();
//                PjsipService.currentCall.answer();
//                showCall();
//            }
            CallOpParam param = new CallOpParam();
            param.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
            try {
                PjsipService.currentCall.answer(param);
            } catch (Exception e) {
                e.printStackTrace();
            }
            showCall();
        });
        declineButton.setOnClickListener(v -> endCall());

        parent = findViewById(R.id.activity_dial);
        refreshCounterThread = new UpdateTextViewThread();


        if (PjsipService.currentCall != null) {
            try {
                if (PjsipService.currentCall.getInfo().getRole() == pjsip_role_e.PJSIP_ROLE_UAS) {
                    displayCall();

                } else {
//                    SipHelper.getInstance().call(getIntent().getStringExtra("sip"), listener);
                    tryToDial();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }


    }

    private void showCall() {
        runOnUiThread(() -> {
            seconds = 0;
            Log.d("***", "starting call");
            updateStatus("Calling");
            clockView.setVisibility(View.VISIBLE);
            if (refreshCounterThread != null) {
                refreshCounterThread.interrupt();
            }
            refreshCounterThread = new UpdateTextViewThread();
            refreshCounterThread.start();
            parent.setBackgroundResource(R.drawable.dial_screen_call_active_background);
            hangupButton.setVisibility(View.VISIBLE);
            ((View) takeButton.getParent()).setVisibility(View.GONE);

            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }

            if (thePlayer != null)
                thePlayer.release();
        });

    }



    @Subscribe
    public void onCallStateUpdated(PjsipService.CallStateEvent event){
        runOnUiThread(() -> {
            CallInfo ci = event.callInfo;
            if (ci.getRole() == pjsip_role_e.PJSIP_ROLE_UAC) {
                updateStatus("Ringing..");
            }

            if (ci.getState().swigValue() <
                    pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()) {
                if (ci.getRole() == pjsip_role_e.PJSIP_ROLE_UAS) {
                    updateStatus("Incoming call..");
                } else {
                    updateStatus(ci.getStateText().equalsIgnoreCase("EARLY") ?  "Ringing..." : ci.getStateText());
                }
            } else if (ci.getState().swigValue() >= pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()) {
                updateStatus(ci.getStateText().equalsIgnoreCase("EARLY") ?  "Ringing..." : ci.getStateText());
                if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                    showCall();
                } else if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                    endCall();
                    updateStatus("Call disconnected: " + ci.getLastReason());
                }
            }
        });

    }

    private void endCall(){
        endCall("Ended");
    }
    private void endCall(String message) {
        runOnUiThread(() -> {

            parent.setBackgroundResource(R.drawable.dial_screen_background);
            hangupButton.setVisibility(View.GONE);
            ((View) takeButton.getParent()).setVisibility(View.GONE);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            if (thePlayer != null)
                thePlayer.release();

            refreshCounterThread.setInterrupted(true);

            Handler handler = new Handler();
            handler.postDelayed(DialActivity.this::finish, 2000);
        });
        Log.d("***", "ending call");
        updateStatus(message);

        try {
            if (PjsipService.currentCall != null &&
                    PjsipService.currentCall.getInfo().getRole() == pjsip_role_e.PJSIP_ROLE_UAC &&
                    PjsipService.currentCall.getInfo().getState().swigValue() > pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()){

                CallOpParam param = new CallOpParam();
                param.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
                PjsipService.currentCall.hangup(param);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            PjsipService.currentCall.delete();
        } catch (Exception e){
            e.printStackTrace();
        }

        PjsipService.currentCall = null;

    }

    private void displayCall() {

        Log.d("***", "receiving call");
        clockView.setVisibility(View.GONE);
        updateStatus("Ringing");
        parent.setBackgroundResource(R.drawable.dial_screen_background);
        hangupButton.setVisibility(View.GONE);
        ((View) takeButton.getParent()).setVisibility(View.VISIBLE);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        thePlayer = MediaPlayer.create(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));

        try {
            thePlayer.setVolume((float) (audioManager.getStreamVolume(AudioManager.STREAM_RING) / 7.0),
                    (float) (audioManager.getStreamVolume(AudioManager.STREAM_RING) / 7.0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        thePlayer.start();

    }

    private void tryToDial() {
        clockView.setVisibility(View.GONE);
        Log.d("***", "waiting for opponent to take call");
        updateStatus("Waiting for opponent");
        parent.setBackgroundResource(R.drawable.dial_screen_background);
        hangupButton.setVisibility(View.VISIBLE);
        ((View) takeButton.getParent()).setVisibility(View.GONE);

    }

    private void updateStatus(String status) {
        statusView.setText(status);
        try {
            Log.d("pjsip", PjsipService.currentCall.getInfo().getStateText() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class UpdateTextViewThread extends Thread {
        private boolean interrupted = false;

        public void setInterrupted(boolean isInterrupted) {
            interrupted = isInterrupted;
        }

        @Override
        public void run() {
            try {
                while (!interrupted) {
                    runOnUiThread(() -> {
                        String passedTime = "" + seconds / 60;
                        if (passedTime.length() < 2)
                            passedTime = "0" + passedTime;
                        String passedSeconds = seconds % 60 + "";
                        if (passedSeconds.length() < 2)
                            passedSeconds = "0" + passedSeconds;
                        passedTime += ":" + passedSeconds;
                        clockView.setText(passedTime);
                        clockView.invalidate();
                        seconds++;
                    });
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        PowerManager.WakeLock wl = ((PowerManager)getSystemService(POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
        wl.acquire();

    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

}

