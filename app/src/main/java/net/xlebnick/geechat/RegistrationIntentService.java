package net.xlebnick.geechat;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import net.xlebnick.geechat.utils.ServerHelper;
import net.xlebnick.geechat.utils.Utils;

import org.jivesoftware.smackx.vcardtemp.VCardManager;

import java.io.IOException;

/**
 * Created by XlebNick for CMessenger.
 */
public class RegistrationIntentService extends IntentService {

    public RegistrationIntentService() {
        super("net.xlebnick.geechat.gcmregistrationservice");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.d("***", "token" + token);
            Utils.saveGcmToken(this, token);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
