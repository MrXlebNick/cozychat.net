package com.messiah.messenger.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.messiah.messenger.R;
import com.messiah.messenger.adapter.UserAdapter;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.model.User;
import com.messiah.messenger.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import xdroid.toaster.Toaster;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class UserListFragment extends LoadableFragment {


    private final static int PICK_CONTACT = 100;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 99;

    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private XmppHelper connection;
    private FloatingActionButton fab;

    private boolean isActive = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UserListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_CONTACT) {
                onLoadStart();
            }
        }
    }

//    public boolean getIsDialogs(){
//        return getArguments().getBoolean("isDialogs");
//    }

    @Override
    protected void onLoadStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            return;

        }
        new AsyncTask<Void, Void, Void>() {
            List<User> usrs;
            List<User> contacts;
            List<User> friends = new ArrayList<>();

            @Override
            protected Void doInBackground(Void... params) {
                connection = XmppHelper.getInstance();
                usrs = connection.getAllUsers();
                return null;
            }

            @Override
            protected void onPreExecute() {

                contacts = Utils.getAllContacts(getContext());
                super.onPreExecute();

            }

            @Override
            protected void onPostExecute(Void aVoid) {

                if (usrs != null) {
                    for (User user : usrs) {
                        for (User contact : contacts) {
                            if (user.mPhoneNumber.equals(contact.mPhoneNumber) && !user.mPhoneNumber.equals(Utils.getPhoneNumber(getContext()))) {
                                user.mFullName = contact.mFullName;
//                                if (unreadFrom.contains(user.mPhoneNumber)){
//                                    user.hasUnread = true;
//                                }
                                friends.add(user);
                                break;
                            }
                        }
                        user.save();
                    }

                    ((UserAdapter) recyclerView.getAdapter()).setValues(usrs);

                    fab.setOnClickListener(v -> {
                        View dialogLayout = View.inflate(getContext(), R.layout.invite_friend_dialog, null);
                        final EditText editText = (EditText) dialogLayout.findViewById(R.id.et_phone);
                        new AlertDialog.Builder(getContext())
                                .setView(dialogLayout)
                                .setTitle(R.string.enter_phone)
                                .setPositiveButton(R.string.invite,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String invitingNumber = editText.getText().toString();
                                                if (invitingNumber.charAt(0) == '8')
                                                    invitingNumber = "+7" + invitingNumber.substring(1);

                                                for (User user : usrs) {
                                                    if (user.mPhoneNumber.equals(invitingNumber)) {

                                                        if (friends.contains(user)) {
                                                            Toaster.toast(getString(R.string.user_already_in_contact_list));
                                                            return;
                                                        }

                                                        Toaster.toast(getString(R.string.user_is_registered_but_not_friended));

                                                        Intent intent = new Intent(Intent.ACTION_INSERT);
                                                        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

                                                        intent.putExtra(ContactsContract.Intents.Insert.NAME, user.mFullName);
                                                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, user.mPhoneNumber);

                                                        startActivityForResult(intent, PICK_CONTACT);
                                                        return;
                                                    }
                                                }


                                                Toaster.toast(getString(R.string.user_not_using_app));
                                                Intent intent = new Intent(Intent.ACTION_SENDTO);
                                                intent.setData(Uri.parse("smsto:" + invitingNumber));
                                                intent.putExtra("sms_body", "Hello! I'm using CMessenger, it's just a miracle! Give it a try!");
                                                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                                    startActivity(intent);
                                                }
                                            }
                                        })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    });
                    onLoaded();
                } else {
                    onFailed();
                }


                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                onLoadStart();
            } else {
                Toaster.toast("Until you grant the permission, we cannot display the names");
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        UserAdapter adapter = new UserAdapter(mListener);
        recyclerView.setAdapter(adapter);

//        XmppHelper.getInstance(getContext()).addObserver(this);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        return super.onCreateView(inflater, container, savedInstanceState, view);
    }

    @Override
    public void onResume() {
        super.onResume();

        fab.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        isActive = true;
        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getContext().getString(R.string.contacts));
            Log.d("***", "here's my title " + getContext().getString(R.string.contacts));
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        isActive = false;
        fab.setVisibility(View.GONE);
        super.onPause();
    }

//    @Override
//    public void update(Observable o, Object arg) {
//
//        if (isActive)
//            onLoadStart();
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(User item);
    }

}
