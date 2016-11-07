package cozychat.xlebnick.com.cmessenger.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import cozychat.xlebnick.com.cmessenger.R;
import cozychat.xlebnick.com.cmessenger.activity.MainActivity;

/**
 * Created by XlebNick for CMessenger.
 */

public class Utils {
    private static final String DB_NAME = "cozychat.net";
    private static final String SPREFS_FIELD_PHONE = "phone";
    public static void putPhoneNumber(Context context, String phoneNumber){

        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString( SPREFS_FIELD_PHONE, phoneNumber).apply();
    }

    public static String getPhoneNumber(Context context){
        String phone = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_PHONE, null);
        return phone;
    }

    public static void sendNotification(Context context, String from, String body){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_message_text_white)
                        .setContentTitle("Message from " + from)
                        .setContentText(body + "");
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1288, mBuilder.build());

    }
}
