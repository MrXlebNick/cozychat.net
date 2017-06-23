package com.messiah.messenger;

import android.app.Application;
import android.content.IntentFilter;
import android.widget.Toast;

import com.messiah.messenger.model.User;
import com.messiah.messenger.receiver.IncomingCallReceiver;
import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by XlebNick for CMessenger.
 */

@ReportsCrashes(
        mailTo = "xlebnikne@list.ru",
        mode = ReportingInteractionMode.SILENT,
        resToastText = R.string.crash_report_sent
)
public class CozyChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        IncomingCallReceiver callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        OomExceptionHandler.install(this);

//        ACRA.init(this);
    }

    @Override
    public void onTerminate() {
        SugarContext.terminate();
        SchemaGenerator schemaGenerator = new SchemaGenerator(getApplicationContext());
        schemaGenerator.deleteTables(new SugarDb(getApplicationContext()).getDB());
        SugarContext.init(getApplicationContext());
        schemaGenerator.createDatabase(new SugarDb(getApplicationContext()).getDB());
        Toast.makeText(this, "TERMINATE", Toast.LENGTH_SHORT).show();
        super.onTerminate();
    }
}
