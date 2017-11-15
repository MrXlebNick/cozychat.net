package com.messiah.messenger.service;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.messiah.messenger.helpers.XmppHelper;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by XlebNick for CMessenger.
 */
public class MyGcmListenerService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("***", "GCM message received " + remoteMessage.getData().get("message"));
        try {
            XmppHelper.getInstance().newIncomingMessage(null, PacketParserUtils.parseStanza(remoteMessage.getData().get("message")), null);
        } catch (XmlPullParserException | IOException | SmackException e) {
            e.printStackTrace();
        } catch (Exception e) {

        }
        startService(new Intent(this, XmppService.class));
    }

}
