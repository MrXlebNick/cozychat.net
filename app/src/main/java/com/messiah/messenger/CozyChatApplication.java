package com.messiah.messenger;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.widget.Toast;

import com.messiah.messenger.receiver.IncomingCallReceiver;
import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.pjsip_transport_type_e;

/**
 * Created by XlebNick for CMessenger.
 */

@ReportsCrashes(
        mailTo = "xlebnikne@list.ru",
        mode = ReportingInteractionMode.SILENT,
        resToastText = R.string.crash_report_sent
)
public class CozyChatApplication extends android.support.multidex.MultiDexApplication {

    static {
        try{
            System.loadLibrary("openh264");
            // Ticket #1937: libyuv is now included as static lib
            //System.loadLibrary("yuv");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("UnsatisfiedLinkError: " + e.getMessage());
            System.out.println("This could be safely ignored if you " +
                    "don't need video.");
        }
        System.loadLibrary("pjsua2");
        System.out.println("Library loaded");
    }

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);

//        ACRA.init(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        IncomingCallReceiver callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        OomExceptionHandler.install(this);
        context = this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    public static Context getContext() {
        return context;
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
