package com.messiah.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

import com.messiah.messenger.helpers.ServerHelper;
import com.messiah.messenger.helpers.SipHelper;
import com.messiah.messenger.utils.Utils;

/**
 * Created by XlebNick for CMessenger.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isAvailable() || mobile.isAvailable()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ServerHelper.getInstance(context).initConnection(Utils.getPhoneNumber(context));
                    SipHelper.getInstance().register();
                    return null;
                }
            }.execute();

            Log.d("Network Available ", "Flag No 1");
        }
    }
}
