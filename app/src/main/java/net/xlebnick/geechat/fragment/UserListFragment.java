package net.xlebnick.geechat.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import net.xlebnick.geechat.R;
import net.xlebnick.geechat.adapter.UserAdapter;
import net.xlebnick.geechat.model.Message;
import net.xlebnick.geechat.model.User;
import net.xlebnick.geechat.utils.ServerHelper;
import net.xlebnick.geechat.utils.Utils;
import xdroid.toaster.Toaster;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class UserListFragment extends LoadableFragment implements Observer {


    private final static int PICK_CONTACT = 100;

    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private ServerHelper connection;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        UserAdapter adapter = new UserAdapter(mListener);
        recyclerView.setAdapter(adapter);

        ServerHelper.getInstance(getContext()).addObserver(this);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:"));  // This ensures only SMS apps respond
                intent.putExtra("sms_body", "Hello! I'm using CMessenger, it's just a miracle! Give it a try!");
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        return super.onCreateView(inflater, container, savedInstanceState, view);
    }

    @Override
    public void onResume() {
        super.onResume();

        isActive = true;
        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Контакты");
        } catch (Exception ignored){}
    }

    @Override
    public void onPause() {
        isActive = false;
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    protected void onLoadStart() {
        new AsyncTask<Void, Void, Void>() {
            List<User> usrs;
            List<User> contacts;
            List<User> friends = new ArrayList<>();


            @Override
            protected void onPreExecute() {
                contacts = Utils.getAllContacts(getContext());
                super.onPreExecute();

            }

            @Override
            protected Void doInBackground(Void... params) {
                connection = ServerHelper.getInstance(getContext());
                usrs = connection.getAllUsers();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid)    {
                if (usrs != null){

                    Set<String> unreadFrom = new HashSet<String>();
                    List<Message> unreadMessages = Message.findWithQuery(Message.class,
                            "SELECT * FROM Message WHERE read = 0 ORDER BY time");
                    for (Message message : unreadMessages){
                        unreadFrom.add(message.sender);
                    }
                    for (User user : usrs){
                        for (User contact : contacts){
                            if (user.mPhoneNumber.equals(contact.mPhoneNumber) && !user.mPhoneNumber.equals(Utils.getPhoneNumber(getContext()))){
                                user.mFullName = contact.mFullName;
                                if (unreadFrom.contains(user.mPhoneNumber)){
                                    user.hasUnread = true;
                                }
                                friends.add(user);
                                break;
                            }
                        }
                    }

                    ((UserAdapter) recyclerView.getAdapter()).setValues(friends);

                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View dialogLayout = View.inflate(getContext(), R.layout.invite_friend_dialog, null);
                            final EditText editText = (EditText) dialogLayout.findViewById(R.id.et_phone);
                            new AlertDialog.Builder(getContext())
                                    .setView(dialogLayout)
                                    .setTitle("Enter phone")
                                    .setPositiveButton("Invite",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String invitingNumber = editText.getText().toString();
                                                    if (invitingNumber.charAt(0) == '8')
                                                        invitingNumber = "+7" + invitingNumber.substring(1);

                                                    for (User user : usrs){
                                                        if (user.mPhoneNumber.equals(invitingNumber)){

                                                            if (friends.contains(user)){
                                                                Toaster.toast("User is already in your friend list");
                                                                return;
                                                            }

                                                            Toaster.toast("User is registered, but missing in your contacts");

                                                            Intent intent = new Intent(Intent.ACTION_INSERT);
                                                            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

                                                            intent.putExtra(ContactsContract.Intents.Insert.NAME, user.mFullName);
                                                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, user.mPhoneNumber);

                                                            startActivityForResult(intent, PICK_CONTACT);
                                                            return;
                                                        }
                                                    }


                                                    Toaster.toast("User is not using CMessenger, invite!");
                                                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                                                    intent.setData(Uri.parse("smsto:" + invitingNumber));
                                                    intent.putExtra("sms_body", "Hello! I'm using CMessenger, it's just a miracle! Give it a try!");
                                                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                                        startActivity(intent);
                                                    }
                                                }
                                            })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == PICK_CONTACT){
                onLoadStart();
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {

        if (isActive)
            onLoadStart();
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
