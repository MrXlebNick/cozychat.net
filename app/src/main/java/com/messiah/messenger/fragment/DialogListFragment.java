package com.messiah.messenger.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.adapter.DialogAdapter;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.model.Dialog;
import com.messiah.messenger.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link UserListFragment.OnListFragmentInteractionListener}
 * interface.
 */
public class DialogListFragment extends LoadableFragment implements Observer {

    private UserListFragment.OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private boolean isActive = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DialogListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UserListFragment.OnListFragmentInteractionListener) {
            mListener = (UserListFragment.OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_list, container, false);

        Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        emptyView = (TextView) view.findViewById(R.id.empty);
        DialogAdapter adapter = new DialogAdapter(mListener);
        recyclerView.setAdapter(adapter);


        Message.findById(Message.class, 1);
        return super.onCreateView(inflater, container, savedInstanceState, view);
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getContext().getString(R.string.dialogs));
        } catch (Exception ignored) {}
        isActive = true;
        Log.d("update", "isActive = " + isActive);
    }

    @Override
    public void onPause() {
        super.onPause();

        isActive = false;

        Log.d("update", "isActive = " + isActive);
    }

    @Override
    public void onStop() {
        XmppHelper.getInstance().deleteObserver(this);
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    protected void onLoadStart() {

        Log.d("***", "update dialogs");
        List<Message> messages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message ORDER BY time");
        List<Dialog> dialogs = new ArrayList<>();
        for (Message message : messages) {
            boolean isDialogExist = false;
            for (Dialog dialog : dialogs) {
                if ((message.isFromMe ? message.receiver : message.sender).equals(dialog.peer.mPhoneNumber)) {
                    dialog.addMessage(message);
                    isDialogExist = true;
                    break;
                }
            }
            if (!isDialogExist) {
                Dialog dialog = new Dialog(getContext(), message.isFromMe ? message.receiver : message.sender);
                dialog.addMessage(message);
                dialogs.add(dialog);
            }

        }
        emptyView.setVisibility(dialogs.size() == 0 ? View.VISIBLE : View.GONE);

        ((DialogAdapter) recyclerView.getAdapter()).setValues(dialogs);
        onLoaded();

    }

    @Override
    public void onStart() {
        super.onStart();
        new Thread(() -> XmppHelper.getInstance().addObserver(DialogListFragment.this)).start();
    }

    @Override
    public void update(Observable o, Object arg) {

        Log.d("***", "DialogListFragment got the message");
        if (isActive)
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    load();
                }
            });
    }
}
