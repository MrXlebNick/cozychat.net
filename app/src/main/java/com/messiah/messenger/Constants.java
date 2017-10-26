package com.messiah.messenger;

/**
 * Created by AKiniyalocts on 2/23/15.
 */
public class Constants {
    /*
      Logging flag
     */
    public static final boolean LOGGING = false;

    /*
      Your imgur client id. You need this to upload to imgur.

      More here: https://api.imgur.com/
     */
    public static final String MY_IMGUR_CLIENT_ID = "1fab7de01828895";
    public static final String MY_IMGUR_CLIENT_SECRET = "6893e5fde3e5cc910068b57b36db526d939abb31";

    /*
      Redirect URL for android.
     */
    public static final String MY_IMGUR_REDIRECT_URL = "http://android";
    public static final String FILE_MULTIPART_NAME = "file";
    public static final String MESSAGE_FILE_PARTS_SEPARATOR = "@";
    public static final String MESSAGE_TYPE_CALL = "file";
    public static final String MESSAGE_TYPE_FILE = "file";
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String SIP_SERVER_HOSTNAME = "ec2-35-162-177-84.us-west-2.compute.amazonaws.com";
    public static final String SIP_SERVER_ADDRESS = "35.162.177.84";


    /*
      Client Auth
     */
    public static String getClientAuth() {
        return "Client-ID " + MY_IMGUR_CLIENT_ID;
    }

    public static final String MESSAGE_FILE_INDEX_PREFIX = "com.messiah.messenger.file.suffix";

}
