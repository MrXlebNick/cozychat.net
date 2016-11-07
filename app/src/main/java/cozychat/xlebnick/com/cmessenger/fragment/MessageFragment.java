package cozychat.xlebnick.com.cmessenger.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.gson.Gson;
import com.orm.query.Select;

import java.util.List;

import cozychat.xlebnick.com.cmessenger.R;
import cozychat.xlebnick.com.cmessenger.adapter.MessageAdapter;
import cozychat.xlebnick.com.cmessenger.model.Message;
import cozychat.xlebnick.com.cmessenger.utils.ServerHelper;
import cozychat.xlebnick.com.cmessenger.utils.Utils;
import okhttp3.internal.Util;

public class MessageFragment extends LoadableFragment {

    private static final int PICK_IMAGE = 1;
    BroadcastReceiver receiver;

    // TODO: Customize parameter argument names
    private static final String ARG_PHONE_NUMBER = "column-count";
    // TODO: Customize parameters
    private String mPhoneNumber;
    private List<Message> messages;

    RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MessageFragment newInstance(String phoneNumber) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_NUMBER, phoneNumber);
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

        final EditText editText = (EditText) view.findViewById(R.id.edit_text);
        View btnSend = view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerHelper.getInstance(getContext(), mPhoneNumber).sendMessage(getContext(),
                        mPhoneNumber,
                        editText.getText().toString());
                Message messageForDb = new Message();
                messageForDb.sender = Utils.getPhoneNumber(context);
                messageForDb.receiver = mPhoneNumber;
                messageForDb.body = editText.getText().toString();
                messageForDb.time = System.currentTimeMillis();
                messageForDb.isFromMe = true;
                messageForDb.save();
                messages = getMessagesFromDb();
                ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                editText.setText("");
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
        return super.onCreateView(inflater, container, savedInstanceState, view);
    }


    @Override
    protected void onLoadStart() {

        if (getArguments() != null) {
            mPhoneNumber = getArguments().getString(ARG_PHONE_NUMBER);

            try {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mPhoneNumber);
            } catch (Exception ignored){}
        }

        messages = getMessagesFromDb();

        MessageAdapter adapter = new MessageAdapter();
        adapter.setValues(messages);
        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                messages = getMessagesFromDb();
                ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount());

            }
        };
        LocalBroadcastManager.getInstance(getContext()).registerReceiver((receiver),
                new IntentFilter(ServerHelper.MESSAGE_RECEIVED)
        );

        Log.d("shit", "fuck");

        onLoaded();
    }

    private List<Message> getMessagesFromDb(){
        List<Message> messages = Message.findWithQuery(Message.class,
                "SELECT distinct sender FROM Message", null);
        for (Message message : messages){
            Log.d("shitty", message.body);
        }
        return  Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE sender = ? OR receiver = ?  ORDER BY time DESC",
                mPhoneNumber, mPhoneNumber);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
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
        }
        return true;
    }
}
