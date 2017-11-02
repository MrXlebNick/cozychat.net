package com.messiah.messenger.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.R;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.utils.Utils;

import java.io.IOException;

/**
 * Created by XlebNick for CMessenger.
 */
public class RegistrationIntentService extends IntentService {

    public RegistrationIntentService() {
        super("com.messiah.messenger");

        Log.d("***", "reg service ");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.d("***", "token" + token);
            Utils.saveGcmToken(this, token);

            XmppHelper.getInstance().saveGcmToken(Utils.getGcmToken(CozyChatApplication.getContext()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
