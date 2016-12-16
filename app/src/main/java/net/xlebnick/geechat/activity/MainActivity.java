package net.xlebnick.geechat.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.List;

import net.xlebnick.geechat.ListenForMessagesService;
import net.xlebnick.geechat.ProfileInfoFragment;
import net.xlebnick.geechat.R;
import net.xlebnick.geechat.RegistrationIntentService;
import net.xlebnick.geechat.fragment.MessageFragment;
import net.xlebnick.geechat.fragment.UserListFragment;
import net.xlebnick.geechat.model.User;
import net.xlebnick.geechat.utils.Utils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        UserListFragment.OnListFragmentInteractionListener {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static String TAG = "LaunchActivity";
    protected String SENDER_ID = "Your_sender_id";
    private GoogleCloudMessaging gcm =null;
    private String regid = null;
    private Context context= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        fragmentTransaction.replace(R.id.fragmnet_container, UserListFragment.newInstance());
        fragmentTransaction.commit();

        ((TextView) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.textView)).setText(Utils.getPhoneNumber(this));

        startService(new Intent(this, ListenForMessagesService.class));
        startService(new Intent(this, RegistrationIntentService.class));

        if (!TextUtils.isEmpty(getIntent().getStringExtra(Utils.FROM_PHONE))){
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            MessageFragment fragment = MessageFragment.newInstance(getIntent().getStringExtra(Utils.FROM_PHONE), "");
            fragment.setHasOptionsMenu(true);
            fragmentTransaction.replace(R.id.fragmnet_container, fragment);
            fragmentTransaction.addToBackStack(null);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            fragmentTransaction.commit();
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!(getVisibleFragment() instanceof UserListFragment)){
            getSupportFragmentManager().popBackStackImmediate();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        Log.d("shit", "messageId "  + id);

        if (id == R.id.action_settings) {
            return true;
        }
         if (id == R.id.nav_exit){
             finish();
         }

        if (id == android.R.id.home && !(getVisibleFragment() instanceof UserListFragment) ) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;

        if (id == R.id.nav_account){
            fragment = new ProfileInfoFragment();

        } else if (id == R.id.nav_contacts){
            fragment = UserListFragment.newInstance();
        }

        if (fragment != null){

            fragment.setHasOptionsMenu(true);
            fragmentTransaction.replace(R.id.fragmnet_container, fragment);
            fragmentTransaction.addToBackStack(null);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentTransaction.commit();

    }



    public Fragment getVisibleFragment(){
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if(fragments != null){
            for(Fragment fragment : fragments){
                if(fragment != null && fragment.isVisible())
                    return fragment;
            }
        }
        return null;
    }


}
