package cozychat.xlebnick.com.cmessenger.model;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 * Created by XlebNick for CMessenger.
 */

public class Message extends SugarRecord{
    public String body;
    public long time;
    public boolean isFromMe;
    public String receiver;
    public String sender;
    @Unique
    public String id;
    public String type;
}
