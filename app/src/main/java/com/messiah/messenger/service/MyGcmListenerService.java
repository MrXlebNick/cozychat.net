package com.messiah.messenger.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.messiah.messenger.helpers.XmppHelper;

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
            XmppHelper.getInstance().newIncomingMessage(null, PacketParserUtils.parseStanza(bundle.getString("message")), null);
        } catch (XmlPullParserException | IOException | SmackException e) {
            e.printStackTrace();
        } catch (Exception e) {

        }
        startService(new Intent(this, XmppService.class));
    }
}
