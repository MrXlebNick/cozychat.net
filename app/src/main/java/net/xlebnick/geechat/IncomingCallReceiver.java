package net.xlebnick.geechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipManager;

import net.xlebnick.geechat.activity.DialActivity;

/**
 * Created by XlebNick for CMessenger.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startActivityIntent = new Intent(context, DialActivity.class);
        startActivityIntent.putExtras(intent);
        startActivityIntent.putExtra(DialActivity.IS_INCOMING_CALL, true);
        startActivityIntent.putExtra(SipManager.EXTRA_OFFER_SD, intent.getStringExtra(SipManager.EXTRA_OFFER_SD));
        startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startActivityIntent);
    }
}
