package com.messiah.messenger.activity;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.service.PjsipService;

import org.greenrobot.eventbus.EventBus;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.VideoPreviewOpParam;
import org.pjsip.pjsua2.VideoWindowHandle;
import org.pjsip.pjsua2.app.MyApp;
import org.pjsip.pjsua2.pjmedia_orient;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;
import org.pjsip.pjsua2.pjsip_status_code;

class VideoPreviewHandler implements SurfaceHolder.Callback {
    public boolean videoPreviewActive = false;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }    public void updateVideoPreview(SurfaceHolder holder) {
        if (PjsipService.currentCall != null &&
                PjsipService.currentCall.vidWin != null &&
                PjsipService.currentCall.vidPrev != null) {
            if (videoPreviewActive) {
                VideoWindowHandle vidWH = new VideoWindowHandle();
                vidWH.getHandle().setWindow(holder.getSurface());
                VideoPreviewOpParam vidPrevParam = new VideoPreviewOpParam();
                vidPrevParam.setWindow(vidWH);
                try {
                    PjsipService.currentCall.vidPrev.start(vidPrevParam);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    PjsipService.currentCall.vidPrev.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        updateVideoPreview(holder);
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            PjsipService.currentCall.vidPrev.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class CallActivity extends Activity
        implements SurfaceHolder.Callback {

    private static VideoPreviewHandler previewHandler =
            new VideoPreviewHandler();
    private static CallInfo lastCallInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        SurfaceView surfaceInVideo = (SurfaceView)
                findViewById(R.id.surfaceIncomingVideo);
        SurfaceView surfacePreview = (SurfaceView)
                findViewById(R.id.surfacePreviewCapture);
        Button buttonShowPreview = (Button)
                findViewById(R.id.buttonShowPreview);

        if (PjsipService.currentCall == null ||
                PjsipService.currentCall.vidWin == null) {
            surfaceInVideo.setVisibility(View.GONE);
            buttonShowPreview.setVisibility(View.GONE);
        }
        setupVideoPreview(surfacePreview, buttonShowPreview);
        surfaceInVideo.getHolder().addCallback(this);
        surfacePreview.getHolder().addCallback(previewHandler);

        if (PjsipService.currentCall != null) {
            try {
                lastCallInfo = PjsipService.currentCall.getInfo();
                updateCallState(lastCallInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            updateCallState(lastCallInfo);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        WindowManager wm;
        Display display;
        int rotation;
        pjmedia_orient orient;

        wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        rotation = display.getRotation();
        System.out.println("Device orientation changed: " + rotation);

        switch (rotation) {
            case Surface.ROTATION_0:   // Portrait
                orient = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG;
                break;
            case Surface.ROTATION_90:  // Landscape, home button on the right
                orient = pjmedia_orient.PJMEDIA_ORIENT_NATURAL;
                break;
            case Surface.ROTATION_180:
                orient = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_90DEG;
                break;
            case Surface.ROTATION_270: // Landscape, home button on the left
                orient = pjmedia_orient.PJMEDIA_ORIENT_ROTATE_180DEG;
                break;
            default:
                orient = pjmedia_orient.PJMEDIA_ORIENT_UNKNOWN;
        }

        if (MyApp.ep != null && PjsipService.account != null) {
            try {
                AccountConfig cfg = PjsipService.account.cfg;
                int cap_dev = cfg.getVideoConfig().getDefaultCaptureDevice();
                MyApp.ep.vidDevManager().setCaptureOrient(cap_dev, orient,
                        true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setupVideoPreview(SurfaceView surfacePreview,
                                  Button buttonShowPreview) {
        surfacePreview.setVisibility(previewHandler.videoPreviewActive ?
                View.VISIBLE : View.GONE);

        buttonShowPreview.setText(previewHandler.videoPreviewActive ?
                "Hide Preview":
                "Show Preview");
    }    private void updateVideoWindow(boolean show) {
        if (PjsipService.currentCall != null &&
                PjsipService.currentCall.vidWin != null &&
                PjsipService.currentCall.vidPrev != null) {
            SurfaceView surfaceInVideo = (SurfaceView)
                    findViewById(R.id.surfaceIncomingVideo);

            VideoWindowHandle vidWH = new VideoWindowHandle();
            if (show) {
                vidWH.getHandle().setWindow(
                        surfaceInVideo.getHolder().getSurface());
            } else {
                vidWH.getHandle().setWindow(null);
            }
            try {
                PjsipService.currentCall.vidWin.setWindow(vidWH);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateCallState(CallInfo ci) {
        TextView tvPeer = (TextView) findViewById(R.id.textViewPeer);
        TextView tvState = (TextView) findViewById(R.id.textViewCallState);
        Button buttonHangup = (Button) findViewById(R.id.buttonHangup);
        Button buttonAccept = (Button) findViewById(R.id.buttonAccept);
        String call_state = "";

        if (ci.getRole() == pjsip_role_e.PJSIP_ROLE_UAC) {
            buttonAccept.setVisibility(View.GONE);
        }

        if (ci.getState().swigValue() <
                pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()) {
            if (ci.getRole() == pjsip_role_e.PJSIP_ROLE_UAS) {
                call_state = "Incoming call..";
		/* Default button texts are already 'Accept' & 'Reject' */
            } else {
                buttonHangup.setText("Cancel");
                call_state = ci.getStateText();
            }
        } else if (ci.getState().swigValue() >=
                pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()) {
            buttonAccept.setVisibility(View.GONE);
            call_state = ci.getStateText();
            if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                buttonHangup.setText("Hangup");
            } else if (ci.getState() ==
                    pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                buttonHangup.setText("OK");
                call_state = "Call disconnected: " + ci.getLastReason();
            }
        }

        tvPeer.setText(ci.getRemoteUri());
        tvState.setText(call_state);
    }    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        updateVideoWindow(true);
    }

    public void acceptCall(View view) {
        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
        try {
            PjsipService.currentCall.answer(prm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        view.setVisibility(View.GONE);
    }    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void hangupCall(View view) {
        finish();

        if (PjsipService.currentCall != null) {
            CallOpParam prm = new CallOpParam();
            prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
            try {
                PjsipService.currentCall.hangup(prm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }    public void surfaceDestroyed(SurfaceHolder holder) {
        updateVideoWindow(false);
    }

    public void showPreview(View view) {
        SurfaceView surfacePreview = (SurfaceView)
                findViewById(R.id.surfacePreviewCapture);

        Button buttonShowPreview = (Button)
                findViewById(R.id.buttonShowPreview);


        previewHandler.videoPreviewActive = ! previewHandler.videoPreviewActive;

        setupVideoPreview(surfacePreview, buttonShowPreview);

        previewHandler.updateVideoPreview(surfacePreview.getHolder());
    }

    public void onCallStateUpdated(PjsipService.CallStateEvent event){

        lastCallInfo = event.callInfo;
        updateCallState(lastCallInfo);
    }

    public void onCallMediaStateUpdated(PjsipService.CallMediaStateEvent event){
        if (PjsipService.currentCall.vidWin != null) {
        /* Set capture orientation according to current
		 * device orientation.
		 */
            onConfigurationChanged(getResources().getConfiguration());
		/* If there's incoming video, display it. */
            setupVideoSurface();
        }
    }



    private void setupVideoSurface() {
        SurfaceView surfaceInVideo = (SurfaceView)
                findViewById(R.id.surfaceIncomingVideo);
        SurfaceView surfacePreview = (SurfaceView)
                findViewById(R.id.surfacePreviewCapture);
        Button buttonShowPreview = (Button)
                findViewById(R.id.buttonShowPreview);
        surfaceInVideo.setVisibility(View.VISIBLE);
        buttonShowPreview.setVisibility(View.VISIBLE);
        surfacePreview.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
