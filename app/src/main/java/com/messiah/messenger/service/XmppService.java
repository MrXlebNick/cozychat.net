package com.messiah.messenger.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.messiah.messenger.helpers.XmppHelper;

/**
 * Created by XlebNick for CMessenger.
 */
public class XmppService extends Service {


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
                try {
                    XmppHelper.getInstance().setMessageListener();
                } catch (NullPointerException e){
                    e.printStackTrace();
                    stopSelf();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    XmppHelper.getInstance().setMessageListener();
                } catch (NullPointerException e){
                    e.printStackTrace();
                    stopSelf();
                }
            }
        }.execute();
        return START_NOT_STICKY;
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
