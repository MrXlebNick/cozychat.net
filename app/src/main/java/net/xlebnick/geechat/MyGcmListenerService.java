package net.xlebnick.geechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import net.xlebnick.geechat.utils.ServerHelper;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by XlebNick for CMessenger.
 */
public class MyGcmListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String s, Bundle bundle) {
        super.onMessageReceived(s, bundle);
        Log.d("***", "GCM message received " + s);
        try {
            ServerHelper.getInstance(this).processMessage(null, ((Message) PacketParserUtils.parseStanza(s)));
        } catch (XmlPullParserException | IOException | SmackException e) {
            e.printStackTrace();
        }
        startService(new Intent(this, ListenForMessagesService.class));
    }
}
