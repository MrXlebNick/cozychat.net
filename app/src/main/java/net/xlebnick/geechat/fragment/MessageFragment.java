package net.xlebnick.geechat.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.sip.SipManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.internal.NavigationMenuPresenter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import net.xlebnick.geechat.R;
import net.xlebnick.geechat.activity.DialActivity;
import net.xlebnick.geechat.adapter.MessageAdapter;
import net.xlebnick.geechat.model.Message;
import net.xlebnick.geechat.utils.ServerHelper;
import net.xlebnick.geechat.utils.Utils;

import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import static android.app.Activity.RESULT_OK;

public class MessageFragment extends LoadableFragment implements Observer {

    private static final int PICK_IMAGE = 1;
    private static final int PICK_FILE = 2;
    BroadcastReceiver receiver;
    // TODO: Customize parameter argument names
    private static final String ARG_PHONE_NUMBER = "column-count";
    private static final String ARG_SIP_NUMBER = "sip";
    // TODO: Customize parameters
    private String mPhoneNumber;
    private String mSipNumber;

    private List<Message> messages;
    private RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MessageFragment newInstance(String phoneNumber, String sipNumber) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_NUMBER, phoneNumber);
        args.putString(ARG_SIP_NUMBER, sipNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_list, container, false);
        final Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, true));
        recyclerView.setAdapter(new MessageAdapter(getContext()));

        final EditText editText = (EditText) view.findViewById(R.id.edit_text);
        View btnSend = view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<String,Void , Void>(){
                    @Override
                    protected Void doInBackground(String... params) {
                        try {
                            ServerHelper.getInstance(getContext(), mPhoneNumber).sendMessage(mPhoneNumber,
                                    URLEncoder.encode(params[0], "utf-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        messages = getMessagesFromDb();
                        ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                        editText.setText("");
                        recyclerView.scrollBy(5,5);
                        super.onPostExecute(aVoid);
                    }
                }.execute(editText.getText().toString());
            }
        });

        final View btnAttach = view.findViewById(R.id.btn_attach);
        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAttach.performLongClick();
            }
        });
        registerForContextMenu(btnAttach);

        ServerHelper.getInstance(getContext()).addObserver(this);

        return super.onCreateView(inflater, container, savedInstanceState, view);
    }


    @Override
    protected void onLoadStart() {

        if (getArguments() != null) {
            mPhoneNumber = getArguments().getString(ARG_PHONE_NUMBER);
            mSipNumber = getArguments().getString(ARG_SIP_NUMBER);

            try {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mPhoneNumber);
            } catch (Exception ignored){}
        }


        markAllAsRead();
        messages = getMessagesFromDb();

        if (getActivity() != null){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                    recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount());
                    recyclerView.scrollBy(5,5);
                    Utils.dismissNotifications(getContext());
                }
            });

        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("orange", "fromBroadcast ");
//
//                markAllAsRead();
//                messages = getMessagesFromDb();
//                ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);

            }
        };
        LocalBroadcastManager.getInstance(getContext()).registerReceiver((receiver),
                new IntentFilter(ServerHelper.MESSAGE_RECEIVED)
        );


        onLoaded();
    }

    private void markAllAsRead(){
        List<Message> messages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE (sender = ? OR receiver = ?) AND read = 0  ORDER BY time DESC",
                mPhoneNumber, mPhoneNumber);;
        for (Message message : messages){
            message.read = true;
            message.update();
        }

    }

    private List<Message> getMessagesFromDb(){
        return  Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE sender = ? OR receiver = ?  ORDER BY time DESC",
                mPhoneNumber, mPhoneNumber);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, 1, 0, "Call");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (SipManager.isVoipSupported(getContext()) && SipManager.isApiSupported(getContext()) ){

            Intent intent = new Intent(getContext(), DialActivity.class);
            intent.putExtra("sip", mSipNumber);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Your device does not support SIP stack, please wait for update", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, v.getId(), 0, "Choose from Gallery");
        menu.add(0, v.getId(), 1, "Choose from files");
    }


    public boolean onContextItemSelected(MenuItem item) {

        if (item.getOrder() == 0) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("file/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_FILE);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == PICK_IMAGE){
                String path = Utils.getRealPathFromURI(getContext(), data.getData());
                final File file = new File(path);
                final Message message = new Message();
                message.isFromMe = true;
                message.body = file.getAbsolutePath();
                message.time = System.currentTimeMillis();
                message.messageId = file.getName() + message.time;
                message.receiver = mPhoneNumber;
                message.sender = Utils.getPhoneNumber(getContext());
                message.type = Message.TYPE_FILE;
                message.save();
                onLoadStart();
            } else if (requestCode == PICK_FILE){

                final File file = new File(data.getData().getPath());
                final Message message = new Message();
                message.isFromMe = true;
                message.body = file.getAbsolutePath();
                message.time = System.currentTimeMillis();
                message.messageId = file.getName() + message.time;
                message.receiver = mPhoneNumber;
                message.sender = Utils.getPhoneNumber(getContext());
                message.type = Message.TYPE_FILE;
                message.save();
                onLoadStart();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void update(Observable o, Object arg) {

        onLoadStart();
    }
}
