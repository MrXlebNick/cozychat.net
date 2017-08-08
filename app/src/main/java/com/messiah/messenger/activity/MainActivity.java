package com.messiah.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.messiah.messenger.R;
import com.messiah.messenger.helpers.SipHelper;
import com.messiah.messenger.fragment.DialogListFragment;
import com.messiah.messenger.fragment.MessageFragment;
import com.messiah.messenger.fragment.ProfileInfoFragment;
import com.messiah.messenger.fragment.UserListFragment;
import com.messiah.messenger.model.User;
import com.messiah.messenger.service.ListenForMessagesService;
import com.messiah.messenger.service.RegistrationIntentService;
import com.messiah.messenger.utils.Utils;

import java.util.List;

import io.github.rockerhieu.emojicon.EmojiconGridFragment;
import io.github.rockerhieu.emojicon.EmojiconsFragment;
import io.github.rockerhieu.emojicon.emoji.Emojicon;
import io.github.rockerhieu.emojiconize.Emojiconize;


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

        SipHelper.getInstance().register();
        Log.d("***", SipManager.isVoipSupported(this)  + " " + SipManager.isApiSupported(this));
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

        fragmentTransaction.replace(R.id.fragmnet_container, Utils.isFirstTime(this) ? new UserListFragment() : new DialogListFragment());
        fragmentTransaction.commit();

        ((TextView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.textView)).setText(Utils.getPhoneNumber(this));

        startService(new Intent(this, ListenForMessagesService.class));
        startService(new Intent(this, RegistrationIntentService.class));

        if (!TextUtils.isEmpty(getIntent().getStringExtra(Utils.FROM_PHONE))) {
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            MessageFragment fragment = MessageFragment.newInstance(getIntent().getStringExtra(Utils.FROM_PHONE), "");
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

    @Override
    public void onListFragmentInteraction(User item) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        MessageFragment fragment = MessageFragment.newInstance(item.mPhoneNumber, item.mSipNumber);
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
}
