package com.messiah.messenger.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.messiah.messenger.R;
import com.messiah.messenger.activity.MainActivity;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.model.User;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by XlebNick for CMessenger.
 */

public class Utils {
    public static final String FROM_PHONE = "from_phone";
    private static final String DB_NAME = "cozychat.net";
    private static final String SPREFS_FIELD_PHONE = "phone";
    private static final String SPREFS_FIELD_SIP = "sip";
    private static final String SPREFS_FIELD_GCM_TOKEN = "gcm_token";
    private static final String SPREFS_FIELD_IS_FIRST_TIME = "is_first_time";

    public static void putPhoneNumber(Context context, String phoneNumber) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(SPREFS_FIELD_PHONE, phoneNumber).apply();
    }

    public static String getPhoneNumber(Context context) {
        return context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_PHONE, null);
    }

    public static ArrayList<User> getAllContacts(Context context) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = context.getContentResolver().query(uri, projection, null, null, null);

        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        ArrayList<User> users = new ArrayList<>();
        while (people.moveToNext()) {
            User singleUser = new User();
            singleUser.mFullName = people.getString(indexName);
            singleUser.mPhoneNumber = people.getString(indexNumber);
            singleUser.mPhoneNumber = singleUser.mPhoneNumber.replaceAll("[^0-9+]", "");
            if (!TextUtils.isEmpty(singleUser.mPhoneNumber) && singleUser.mPhoneNumber.charAt(0) == '8') {
                singleUser.mPhoneNumber = singleUser.mPhoneNumber.replaceFirst("8", "+7");
            }
            users.add(singleUser);
        }
        return users;

    }


    public static void sendNotification(Context context) {

        List<Message> unreadMessages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE read = 0 ORDER BY time");
        Log.d("***", "notif " + unreadMessages.size());

        String title = "";
        String content = "";
        Log.d("***", unreadMessages.size() + " count");
        if (unreadMessages.size() == 0) {
            Log.d("***", " 0");

            return;
        } else if (unreadMessages.size() == 1) {
            Log.d("***", " 2");
            List<User> user = User.find(User.class, "m_phone_number = ?", unreadMessages.get(0).sender);
            title = user.size() == 0 ? unreadMessages.get(0).sender : user.get(0).mFullName;
            content = unreadMessages.get(0).body;
        } else {

            Log.d("***", " 2");
            Set<String> from = new HashSet<>();
            for (Message message : unreadMessages) {
                List<User> user = User.find(User.class, "m_phone_number = ?", message.sender);
                from.add(user.size() == 0 ? message.sender : user.get(0).mFullName);
            }

            for (String fromPhone : from) {
                title += fromPhone + ", ";

            }

            title = title.substring(0, title.length() - 2);
            content = unreadMessages.size() + " new message";
        }


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_message_text_white)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setContentText(content);
        Intent resultIntent = new Intent(context, MainActivity.class);
        if (unreadMessages.size() == 1) {
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

    public static void dismissNotifications(Context context) {
        try {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.cancelAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void putSipNumber(Context context, String mSipNumber) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(SPREFS_FIELD_SIP, mSipNumber).apply();
    }


    public static String getSipNumber(Context context) {
        return context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_SIP, null);
    }


    public static void saveGcmToken(Context context, String token) {


        SharedPreferences sharedPreferences = context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(SPREFS_FIELD_GCM_TOKEN, token).apply();
    }

    public static String getGcmToken(Context mContext) {
        return mContext.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getString(SPREFS_FIELD_GCM_TOKEN, null);
    }

    public static String getMimeType(String url) {
        String parts[] = url.split("\\.");
        String extension = parts[parts.length - 1];
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
            if (cursor.moveToFirst()) {
                int count = cursor.getColumnCount();
                for (int i = 0; i < count; i++) {
                    Log.d("***", cursor.getColumnName(i) + " = " + cursor.getString(i));
                }
            }
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {


        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String moveFile(Context context, String inputPath, String inputFile) {

        String path = context.getExternalFilesDir(null) + "/" + inputFile;
        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(path);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;


        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

        return path;
    }

    public static void removeAuthData(Context context) {
        context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).edit().remove(SPREFS_FIELD_PHONE).apply();
    }

    public static boolean isFirstTime(Context context) {


        if (!context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).getBoolean(SPREFS_FIELD_IS_FIRST_TIME, true)) {
            return false;
        }
        context.getSharedPreferences(DB_NAME, Context.MODE_PRIVATE).edit().putBoolean(SPREFS_FIELD_IS_FIRST_TIME, false).apply();
        return true;

    }
}
