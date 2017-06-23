package com.messiah.messenger.model;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.R;

import java.util.List;

/**
 * Created by XlebNick for CMessenger.
 */

public class Dialog {
    public int count = 0;
    public String message;
    public User oppponent;
    public boolean isFromMe;
    public Context context;


    public Dialog(Context context, String opponent) {
        this.context = context;

        List<User> users = User.find(User.class, "m_phone_number = ?", opponent);
        if (users == null || users.size() == 0) {
            User user = new User();
            user.mPhoneNumber = opponent;
            user.mFullName = "Secret Spy";
            user.mSipNumber = "0000";
            oppponent = user;
        } else {
            oppponent = users.get(0);

        }
        Log.d("***", new Gson().toJson(oppponent));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Dialog) {
            return ((Dialog) obj).oppponent == oppponent;
        }
        return super.equals(obj);
    }

    public void addMessage(Message message) {
        this.message = (message.isFromMe ? context.getString(R.string.you) + ": " : "") + (message.type.equals(Constants.MESSAGE_TYPE_FILE)? "File sent":  message.body);
        if (!message.read && !message.isFromMe) {

            count++;
            Log.d("***", "unread, " + message.body);
        }
        isFromMe = message.isFromMe;
    }


}
