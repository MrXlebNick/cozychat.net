package com.messiah.messenger.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import com.messiah.messenger.model.SecretDialogData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
public class DialogListFragment extends LoadableFragment {

    private UserListFragment.OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private RecyclerView secretRecyclerView;
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

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return position == 0 ? "Plain" : "Secret";
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return view.findViewById(position == 0 ? R.id.list : R.id.secret_list);
            }
        });

        Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        secretRecyclerView = (RecyclerView) view.findViewById(R.id.secret_list);
        secretRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        emptyView = (TextView) view.findViewById(R.id.empty);
        DialogAdapter adapter = new DialogAdapter(mListener);
        recyclerView.setAdapter(adapter);
        DialogAdapter secretAdapter = new DialogAdapter(mListener);
        secretRecyclerView.setAdapter(secretAdapter);


        Message.findById(Message.class, 1);
        SecretDialogData.findById(SecretDialogData.class, 1);
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
        EventBus.getDefault().unregister(this);
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
                "SELECT * FROM Message WHERE dialog_id IS NULL OR dialog_id = \"\" ORDER BY time");
        List<Dialog> dialogs = groupInDialogs(messages);

        List<Message> secretMessages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE dialog_id IS NOT NULL AND dialog_id != \"\" ORDER BY time");
        List<Dialog> secretDialogs = groupInDialogs(secretMessages);

        emptyView.setVisibility(dialogs.size() == 0 && secretDialogs.size() == 0 ? View.VISIBLE : View.GONE);

        ((DialogAdapter) recyclerView.getAdapter()).setValues(dialogs);
        ((DialogAdapter) secretRecyclerView.getAdapter()).setValues(secretDialogs);
        onLoaded();

    }

    private List<Dialog> groupInDialogs(List<Message> messages) {
        List<Dialog> dialogs = new ArrayList<>();
        for (Message message : messages) {
            boolean isDialogExist = false;
            for (Dialog dialog : dialogs) {
                if (!TextUtils.isEmpty(message.dialogId)) {
                    if (dialog.dialogId.equals(message.dialogId)){
                        dialog.addMessage(message);
                        isDialogExist = true;
                        break;
                    }
                } else {

                    if ((message.isFromMe ? message.receiver : message.sender).equals(dialog.peer.mPhoneNumber)) {
                        dialog.addMessage(message);
                        isDialogExist = true;
                        break;
                    }
                }
            }
            if (!isDialogExist) {
                Dialog dialog = new Dialog(getContext(), message.isFromMe ? message.receiver : message.sender);
                dialog.addMessage(message);
                if (!TextUtils.isEmpty(message.dialogId)){
                    dialog.dialogId = message.dialogId;
                }
                dialogs.add(dialog);
            }

        }
        return dialogs;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void update(Message message) {

        Log.d("***", "DialogListFragment got the message");
        if (isActive)
            getActivity().runOnUiThread(this::load);
    }
}
