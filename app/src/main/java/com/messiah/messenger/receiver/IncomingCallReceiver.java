package com.messiah.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipManager;
import android.util.Log;

import com.messiah.messenger.Constants;
import com.messiah.messenger.activity.DialActivity;
import com.messiah.messenger.helpers.SipHelper;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.utils.Utils;

/**
 * Created by XlebNick for CMessenger.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     *
     * @param context The context under which the receiver is running.
     * @param intent  The intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Message callMessage = new Message();
        callMessage.messageId = intent.getStringExtra(SipManager.EXTRA_CALL_ID);
        callMessage.receiver = Utils.getPhoneNumber(context);
        callMessage.time = System.currentTimeMillis();
        callMessage.type = Constants.MESSAGE_TYPE_CALL;
        callMessage.isFromMe = false;
        callMessage.read = true;
        callMessage.sender = SipHelper.getInstance().getCallee(intent);
//        long callMessageId = callMessage.save();
//        if (callMessageId != -1){

            Log.d("***", "someone is calling " + SipHelper.getInstance().getCallee(intent));
            Intent startActivityIntent = new Intent(context, DialActivity.class);
            startActivityIntent.putExtras(intent);
            startActivityIntent.putExtra(DialActivity.IS_INCOMING_CALL, true);
            Log.d("***", intent.getStringExtra(SipManager.EXTRA_CALL_ID) + " " + intent.getStringExtra(SipManager.EXTRA_OFFER_SD)   );
            startActivityIntent.putExtra(SipManager.EXTRA_OFFER_SD, intent.getStringExtra(SipManager.EXTRA_OFFER_SD));
            startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startActivityIntent);
//        }


    }
}
