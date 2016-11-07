package cozychat.xlebnick.com.cmessenger;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

/**
 * Created by XlebNick for CMessenger.
 */

public class CozyChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);


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
