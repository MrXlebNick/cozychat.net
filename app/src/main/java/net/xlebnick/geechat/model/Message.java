package net.xlebnick.geechat.model;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 * Created by XlebNick for CMessenger.
 */

public class Message extends SugarRecord{

    public final static String TYPE_TEXT = "text";
    public final static String TYPE_FILE = "file";

    public final static String FILE_STATUS_FAILED = "failed";
    public final static String FILE_STATUS_PENDING = "pending";
    public final static String FILE_STATUS_LOADING = "loading";
    public final static String FILE_STATUS_LOADED = "loaded";


    public Message(){
        read = false;
    }
    public String body;
    public long time;
    public boolean isFromMe;
    public String receiver;
    public String sender;
    public boolean read;
    @Unique
    public String messageId;
    public String type;
    public String fileStatus = FILE_STATUS_PENDING ;

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
}
