package cozychat.xlebnick.com.cmessenger;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import cozychat.xlebnick.com.cmessenger.utils.ServerHelper;
import cozychat.xlebnick.com.cmessenger.utils.Utils;

/**
 * Created by XlebNick for CMessenger.
 */
public class ListenForMessagesService extends Service {


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("shit", "service onCreate");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("shit", "service onStartCommand");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                SmackConnection.getInstance(getApplicationContext()).init(Utils.getPhoneNumber(getApplicationContext()),
                        Utils.getPhoneNumber(getApplicationContext()));
                ServerHelper.getInstance(getApplicationContext(), null).setMessageListener(getApplicationContext());

                return null;
            }
        }.execute();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("shit", "service onDestroy");
        super.onDestroy();

    }
}
