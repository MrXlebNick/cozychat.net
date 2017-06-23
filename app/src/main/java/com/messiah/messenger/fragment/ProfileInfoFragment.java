package com.messiah.messenger.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.helpers.ServerHelper;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.HashMap;

import xdroid.toaster.Toaster;

public class ProfileInfoFragment extends LoadableFragment {

    private TextView nameView;
    private TextView phoneView;
    private TextView emailView;

    private HashMap<String, String> properties;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile_info, container, false);

        nameView = (TextView) view.findViewById(R.id.name);
        phoneView = (TextView) view.findViewById(R.id.phone);
        emailView = (TextView) view.findViewById(R.id.email);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogBody = inflater.inflate(R.layout.edit_profile_dialog, container, false);
                final EditText nameEditText = (EditText) dialogBody.findViewById(R.id.name);
                final EditText emailEditText = (EditText) dialogBody.findViewById(R.id.email);
                nameEditText.setText(properties.get("username"));
                emailEditText.setText(properties.get("email"));
                new AlertDialog.Builder(getContext())
                        .setView(dialogBody)
                        .setTitle("Edit profile")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!TextUtils.isEmpty(nameEditText.getText().toString()))
                                    properties.put("username", nameEditText.getText().toString());
                                if (!TextUtils.isEmpty(emailEditText.getText().toString()))
                                    properties.put("email", emailEditText.getText().toString());
                                try {
                                    ServerHelper.getInstance(getContext()).setUserProperties(properties);
                                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                                    e.printStackTrace();
                                    Toaster.toast("An error occured, try again. Error: " + e.getMessage());
                                }
                            }
                        }).show();
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState, view);
    }

    @Override
    public void onResume() {

        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(null);
        } catch (Exception ignored) {}
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, 1, 0, "Delete");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (SipManager.isVoipSupported(getContext()) && SipManager.isApiSupported(getContext()) ){
//
//            Intent intent = new Intent(getContext(), DialActivity.class);
//            intent.putExtra("sip", mSipNumber);
//            startActivity(intent);
//        } else {
//            Toast.makeText(getContext(), "Your device does not support SIP stack, please wait for update", Toast.LENGTH_LONG).show();
//        }
        return true;
    }

    @Override
    protected void onLoadStart() {
        try {
            properties = ServerHelper.getInstance(getContext()).getUserProperties();

            nameView.setText(!TextUtils.isEmpty(properties.get("username")) ? properties.get("username") : "Secret Spy");
            phoneView.setText(!TextUtils.isEmpty(properties.get("phone")) ? properties.get("phone") : "Not specified");
            emailView.setText(!TextUtils.isEmpty(properties.get("email")) ? properties.get("email") : "Not specified");
            onLoaded();
        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

}
