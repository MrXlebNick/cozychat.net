package com.messiah.messenger.model;

import android.util.Log;

import com.messiah.messenger.Constants;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 * Created by XlebNick for CMessenger.
 */

public class Message extends SugarRecord {

    public enum Status {SENT, DELIVERED, READ, ERROR}

    public String body;
    public long time;
    public boolean isFromMe;
    public String receiver;
    public String sender;
    public boolean read;
    public String type = Constants.MESSAGE_TYPE_TEXT;
    @Unique
    public String messageId;
    public String dialogId;
    public Status status;
    public String error;

    public Message() {
        read = false;
    }


    @Override
    public long save() {
        if (Message.find(Message.class, "message_id = ?", messageId)
                .isEmpty()) {
            return super.save();
        } else {
            Log.d("***", "try to save duplicates");
        }
        return -1;
    }


    public final static String FILE_STATUS_FAILED = "failed";
    public final static String FILE_STATUS_PENDING = "pending";
    public final static String FILE_STATUS_LOADING = "loading";
    public final static String FILE_STATUS_LOADED = "loaded";

    public String fileStatus = FILE_STATUS_PENDING;
    public String filePath;
    public String fileUri;
    public String fileName;
    public String fileKey;



}
