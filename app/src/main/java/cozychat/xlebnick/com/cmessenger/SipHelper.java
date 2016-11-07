package cozychat.xlebnick.com.cmessenger;

import android.content.Context;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;

import java.text.ParseException;

/**
 * Created by XlebNick for CMessenger.
 */

public class SipHelper {

    public SipManager mSipManager = null;
    public static SipHelper instance = null;
    public SipProfile mSipProfile = null;


    private SipHelper(Context context) {
        if(mSipManager == null) {
            mSipManager = SipManager.newInstance(context);
        }
    }

    public static SipHelper getInstance(Context context) {
        if (instance == null)
            instance = new SipHelper(context);
        return instance;
    }

    public void register(String username, String password) throws ParseException {
        SipProfile.Builder builder = new SipProfile.Builder(username, "78.46.85.86");
        builder.setPassword(password)   ;
        mSipProfile = builder.build();
    }
}
