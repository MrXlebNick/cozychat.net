package com.messiah.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.messiah.messenger.service.XmppService;

/**
 * Created by XlebNick for CMessenger.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, XmppService.class);
        context.startService(startServiceIntent);
    }
}
