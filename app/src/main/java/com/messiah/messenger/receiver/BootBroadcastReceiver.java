package com.messiah.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.messiah.messenger.service.ListenForMessagesService;

/**
 * Created by XlebNick for CMessenger.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, ListenForMessagesService.class);
        context.startService(startServiceIntent);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        IncomingCallReceiver callReceiver = new IncomingCallReceiver();
        context.registerReceiver(callReceiver, filter);
    }
}
