package com.messiah.messenger.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.R;
import com.messiah.messenger.fragment.DialogListFragment;
import com.messiah.messenger.fragment.MessageFragment;
import com.messiah.messenger.fragment.ProfileInfoFragment;
import com.messiah.messenger.fragment.UserListFragment;
import com.messiah.messenger.helpers.SipHelper;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.model.User;
import com.messiah.messenger.service.XmppService;
import com.messiah.messenger.service.PjsipService;
import com.messiah.messenger.service.RegistrationIntentService;
import com.messiah.messenger.utils.CryptoUtils;
import com.messiah.messenger.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import io.github.rockerhieu.emojicon.EmojiconGridFragment;
import io.github.rockerhieu.emojicon.EmojiconsFragment;
import io.github.rockerhieu.emojicon.emoji.Emojicon;
import io.github.rockerhieu.emojiconize.Emojiconize;
import io.reactivex.android.schedulers.AndroidSchedulers;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        UserListFragment.OnListFragmentInteractionListener,
        EmojiconsFragment.OnEmojiconBackspaceClickedListener,
        EmojiconGridFragment.OnEmojiconClickedListener {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static String TAG = "LaunchActivity";
    protected String SENDER_ID = "Your_sender_id";
    private GoogleCloudMessaging gcm = null;
    private String regid = null;
    private Context context = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Emojiconize.activity(this).go();

        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }

        if (TextUtils.isEmpty(Utils.getPhoneNumber(this))){
            Intent intent = new Intent(this, LoginSignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            return;
        }

        startService(new Intent(this, PjsipService.class));

        SipHelper.getInstance().register();
        setContentView(R.layout.activity_template);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.fragmnet_container, new DialogListFragment());
        fragmentTransaction.commit();

        ((TextView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0)
                .findViewById(R.id.text_phone)).setText(Utils.getPhoneNumber(this));

        startService(new Intent(this, XmppService.class));
        startService(new Intent(this, RegistrationIntentService.class));


        XmppHelper.getInstance().getUserPropertiesrx()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(properties -> {
                    ((TextView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0)
                            .findViewById(R.id.text_phone)).setText(Utils.getPhoneNumber(this));

                    ((TextView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0)
                            .findViewById(R.id.text_name)).setText(!TextUtils.isEmpty(properties.get("niknam")) ? properties.get("niknam") : "");

                    if (!TextUtils.isEmpty(properties.get("avatarKey")) && !TextUtils.isEmpty(properties.get("avatarFileName"))) {
                        Picasso.with(MainActivity.this)
                                .load("http://ec2-35-162-177-84.us-west-2.compute.amazonaws.com:8080/" +
                                        properties.get("avatarKey"))
                                .fit()
                                .centerCrop()
                                .into(((ImageView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0)
                                        .findViewById(R.id.imageView)));

                    }
                }, Throwable::printStackTrace);

        if (!TextUtils.isEmpty(getIntent().getStringExtra(Constants.FROM_PHONE))) {
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            MessageFragment fragment = MessageFragment.newInstance(getIntent().getStringExtra(Constants.FROM_PHONE),
                    "",
                    getIntent().getBooleanExtra(Constants.IS_SECRET, false),
                    getIntent().getStringExtra(Constants.SECRET_ID));
            fragment.setHasOptionsMenu(true);
            fragmentTransaction.replace(R.id.fragmnet_container, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == android.R.id.home && !(getVisibleFragment() instanceof UserListFragment)) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Fragment getVisibleFragment() {
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible())
                    return fragment;
            }
        }
        return null;
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!(getVisibleFragment() instanceof UserListFragment)) {
            getSupportFragmentManager().popBackStackImmediate();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;

        if (id == R.id.nav_account) {
            fragment = new ProfileInfoFragment();

        } else if (id == R.id.nav_contacts) {
            fragment = UserListFragment.newInstance();
        } else if (id == R.id.nav_dialogs) {

            fragment = new DialogListFragment();
        }

        if (id == R.id.nav_exit) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("EXIT", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        if (id == R.id.nav_logs) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(this.getLocalClassName(), "Got READ_LOGS permissions");

                sendLogs();
            } else {
                Toast.makeText(this, "READ_LOGS permission was not granted", Toast.LENGTH_SHORT).show();
                Log.e(this.getLocalClassName(), "Don't have READ_LOGS permissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 103);
//                Log.i(this.getLocalClassName(), "new READ_LOGS permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS));
//                return true;

            }


        }

        if (fragment != null) {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            fragment.setHasOptionsMenu(true);
            fragmentTransaction.replace(R.id.fragmnet_container, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void sendLogs() {
        //set a file
        Date datum = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
        String fullName = df.format(datum) + "appLog.log";
        File file = new File(Environment.getExternalStorageDirectory(), fullName);

        //clears a file
        if (file.exists()) {
            file.delete();
        }


        //write log to file
        int pid = android.os.Process.myPid();
        try {
            String command = "logcat -d";
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(String.valueOf(pid))) {
                    result.append(currentLine);
                    result.append("\n");
                }
            }

            FileWriter out = new FileWriter(file);
            out.write(result.toString());
            out.close();

            //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }


        //clear the log
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }


        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        String to[] = {"xlebnikne@list.ru"};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    @Override
    public void onListFragmentInteraction(User item) {

        onListFragmentInteraction(item, null);
    }

    @Override
    public void onListFragmentInteraction(User peer, String dialogId) {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        MessageFragment fragment = MessageFragment.newInstance(peer.mPhoneNumber,
                peer.mSipNumber,
                !TextUtils.isEmpty(dialogId),
                dialogId);
        fragment.setHasOptionsMenu(true);
        fragmentTransaction.replace(R.id.fragmnet_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {

    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "android.permission.USE_SIP",
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE,
                Manifest.permission.READ_CONTACTS}, 103);

    }
}
