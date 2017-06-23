package com.messiah.messenger.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.messiah.messenger.helpers.ServerHelper;

import org.jivesoftware.smack.SmackException;
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
        Log.d("***", "GCM message received " + bundle.getString("message"));
        try {
            ServerHelper serverHelper = ServerHelper.getInstance(this);
            serverHelper.processMessage(null, ((Message) PacketParserUtils.parseStanza(bundle.getString("message"))));
        } catch (XmlPullParserException | IOException | SmackException e) {
            e.printStackTrace();
        }
        startService(new Intent(this, ListenForMessagesService.class));
    }
}
