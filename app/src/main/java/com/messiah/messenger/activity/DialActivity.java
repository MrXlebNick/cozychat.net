package com.messiah.messenger.activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.messiah.messenger.R;
import com.messiah.messenger.helpers.SipHelper;

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
        } catch (Throwable ignored) {
        }

        final SipAudioCall.Listener listener = new SipAudioCall.Listener() {

            @Override
            public void onReadyToCall(SipAudioCall call) {
                Log.d("***", "onReadyToCall");
                super.onReadyToCall(call);
            }

            @Override
            public void onCalling(SipAudioCall call) {
                Log.d("***", "onCalling");
                super.onCalling(call);
            }

            @Override
            public void onRinging(SipAudioCall call, SipProfile caller) {
                Log.d("***", "onRinging");
                super.onRinging(call, caller);
            }

            @Override
            public void onRingingBack(SipAudioCall call) {
                Log.d("***", "onRingingBack");
                super.onRingingBack(call);
            }

            @Override
            public void onCallEstablished(SipAudioCall call) {

                super.onCallEstablished(call);
                Log.d("***", "onCallEstablished");

                call.startAudio();
                Log.d("***", "onCallEstablished1");
                call.setSpeakerMode(true);
                Log.d("***", "onCallEstablished2");
                if (call.isMuted())
                    call.toggleMute();
                Log.d("***", "onCallEstablished3");
                startCall();
                Log.d("***", "onCallEstablished4");

            }

            @Override
            public void onCallEnded(SipAudioCall call) {
                Log.d("***", "onCallEnded");

                try {
                    call.endCall();
                } catch (SipException e) {
                    e.printStackTrace();
                }

                endCall();

            }

            @Override
            public void onCallBusy(SipAudioCall call) {
                Log.d("***", "onCallBusy");
                super.onCallBusy(call);
            }

            @Override
            public void onCallHeld(SipAudioCall call) {
                Log.d("***", "onCallHeld");
                super.onCallHeld(call);
            }

            @Override
            public void onError(SipAudioCall call, int errorCode, final String errorMessage) {
                Log.d("***", "onError " + errorMessage + " " + errorCode);
                DialActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DialActivity.this, "Error has occured, reason: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                endCall();
                super.onError(call, errorCode, errorMessage);
            }

            @Override
            public void onChanged(SipAudioCall call) {
                Log.d("***", "onChanged");
                super.onChanged(call);
            }

        };


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

        hangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SipHelper.getInstance(DialActivity.this).call.endCall();
                } catch (SipException e) {
                    e.printStackTrace();
                }
                endCall();
            }
        });
        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SipHelper.getInstance(DialActivity.this).takeCall(getIntent(), listener);
                startCall();
            }
        });
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SipHelper.getInstance(DialActivity.this).call.endCall();
                } catch (SipException e) {
                    e.printStackTrace();
                }
                endCall();
            }
        });

        parent = findViewById(R.id.activity_dial);
        refreshCounterThread = new UpdateTextViewThread();


        if (getIntent().getExtras() != null) {
            if (getIntent().getBooleanExtra(IS_INCOMING_CALL, false)) {
                displayCall();

            } else {
                Log.d("***", "sip:" + getIntent().getStringExtra("sip") + "@ec2-34-208-141-31.us-west-2.compute.amazonaws.com");
                SipHelper.getInstance(this).call("sip:" + getIntent().getStringExtra("sip") + "@ec2-34-208-141-31.us-west-2.compute.amazonaws.com", listener);
                tryToDial();
            }
        }


    }

    private void startCall() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
            }
        });

    }

    private void endCall() {
        updateStatus("Ended");
        Log.d("***", "ending call");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

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
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DialActivity.this.finish();
                    }
                }, 2000);
            }
        });


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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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

                        }
                    });
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        }

    }

    private void setEndedState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

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
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DialActivity.this.finish();
                    }
                }, 2000);
            }
        });

    }

    private void setIncomingState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


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
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DialActivity.this.finish();
                    }
                }, 2000);
            }
        });

    }

    private void setOutcominState(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

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
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DialActivity.this.finish();
                    }
                }, 2000);
            }
        });
    }



}

