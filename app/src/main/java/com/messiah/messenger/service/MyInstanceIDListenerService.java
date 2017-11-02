package com.messiah.messenger.service;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.messiah.messenger.CozyChatApplication;

/**
 * Created by XlebNick for CMessenger.
 */
public class MyInstanceIDListenerService extends InstanceIDListenerService {

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
        Intent intent = new Intent(CozyChatApplication.getContext(), RegistrationIntentService.class);
        intent.putExtra("register", true); //register=true
        intent.putExtra("tokenRefreshed", true); //tokenRefreshed = true
        CozyChatApplication.getContext().startService(intent);
    }
}
