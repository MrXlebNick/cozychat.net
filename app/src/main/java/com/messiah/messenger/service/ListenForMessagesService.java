package com.messiah.messenger.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.messiah.messenger.helpers.ServerHelper;
import com.messiah.messenger.helpers.SipHelper;

/**
 * Created by XlebNick for CMessenger.
 */
public class ListenForMessagesService extends Service {


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("shit", "service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("shit", "service onStartCommand");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ServerHelper.getInstance(getApplicationContext(), null).
                        setMessageListener();

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                ServerHelper.getInstance(getApplicationContext(), null).
                        setMessageListener();
            }
        }.execute();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
