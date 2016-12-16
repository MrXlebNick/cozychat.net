package net.xlebnick.geechat.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.xlebnick.geechat.R;
import net.xlebnick.geechat.activity.MainActivity;
import net.xlebnick.geechat.model.Message;
import net.xlebnick.geechat.model.User;

/**
 * Created by XlebNick for CMessenger.
 */

public class Utils {
    private static final String DB_NAME = "cozychat.net";
    private static final String SPREFS_FIELD_PHONE = "phone";
    private static final String SPREFS_FIELD_SIP = "sip";
    public static final String FROM_PHONE = "from_phone";
    private static final String SPREFS_FIELD_GCM_TOKEN = "gcm_token";

    public static void putPhoneNumber(Context context, String phoneNumber){

        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString( SPREFS_FIELD_PHONE, phoneNumber).apply();
    }

    public static String getPhoneNumber(Context context){
        return context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_PHONE, null);
    }

    public static ArrayList<User> getAllContacts(Context context) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection    = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = context.getContentResolver().query(uri, projection, null, null, null);

        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        ArrayList<User> users = new ArrayList<>();
        while (people.moveToNext()) {
            User singleUser = new User();
            singleUser.mFullName = people.getString(indexName);
            singleUser.mPhoneNumber= people.getString(indexNumber);
            singleUser.mPhoneNumber = singleUser.mPhoneNumber.replaceAll("[^0-9+]","");
            if (!TextUtils.isEmpty(singleUser.mPhoneNumber) &&singleUser.mPhoneNumber.charAt(0) == '8'){
                singleUser.mPhoneNumber = singleUser.mPhoneNumber.replaceFirst("8", "+7");
            }
            users.add(singleUser);
        }
        return users;

    }


    public static void sendNotification(Context context){
        List<Message> unreadMessages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE read = 0 ORDER BY time");
        Log.d("***", "notif " + unreadMessages.size());
        if (unreadMessages.size() == 0){
            return;
        }

        Set<String> from = new HashSet<>();
        String title = "";
        String content = "";

        for (Message message : unreadMessages){
            from.add(message.sender);
            content = message.body;
        }

        for (String fromPhone : from){
            title += fromPhone + ", ";

        }

        title = title.substring(0, title.length() - 2);

        if (from.size() > 1)
            content = "Tap to open CMessenger";
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_message_text_white)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setContentText(content);
        Intent resultIntent = new Intent(context, MainActivity.class);
        if (unreadMessages.size() == 1){
            resultIntent.putExtra(FROM_PHONE, unreadMessages.get(0).sender);
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(1288, mBuilder.build());

    }

    public static void dismissNotifications(Context context){
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancelAll();

    }

    public static void putSipNumber(Context context, String mSipNumber) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString( SPREFS_FIELD_SIP, mSipNumber).apply();
    }


    public static String getSipNumber(Context context){
        return context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_SIP, null);
    }


    public static void saveGcmToken(Context context, String token) {


        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString( SPREFS_FIELD_GCM_TOKEN, token).apply();
    }

    public static String getGcmToken(Context mContext) {
        return mContext.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_GCM_TOKEN, null);
    }

    public static String getMimeType(String url){
        String parts[]=url.split("\\.");
        String extension=parts[parts.length-1];
        String type = null;
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static String getRealPathFromURI(Context context, Uri contentURI) {
        String result;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            if (cursor.moveToFirst()){
                int count = cursor.getColumnCount();
                for (int i = 0; i < count ; i++){
                    Log.d("***", cursor.getColumnName(i) + " = " + cursor.getString(i));
                }
            }
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
