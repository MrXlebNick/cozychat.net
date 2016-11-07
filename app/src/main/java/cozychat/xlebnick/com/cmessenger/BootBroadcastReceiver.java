package cozychat.xlebnick.com.cmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by XlebNick for CMessenger.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, ListenForMessagesService.class);
        context.startService(startServiceIntent);
    }
}
