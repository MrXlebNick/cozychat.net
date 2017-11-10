package com.messiah.messenger.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.messiah.messenger.CozyChatApplication;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.utils.Utils;

/**
 * Created by XlebNick for CMessenger.
 */
public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("***", "token " + refreshedToken);
        Utils.saveGcmToken(this, refreshedToken);
        try {

            XmppHelper.getInstance().saveGcmToken(Utils.getGcmToken(CozyChatApplication.getContext()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
