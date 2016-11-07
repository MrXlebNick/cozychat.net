package cozychat.xlebnick.com.cmessenger.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import cozychat.xlebnick.com.cmessenger.R;
import cozychat.xlebnick.com.cmessenger.SmackConnection;
import cozychat.xlebnick.com.cmessenger.activity.MainActivity;
import cozychat.xlebnick.com.cmessenger.adapter.UserAdapter;
import cozychat.xlebnick.com.cmessenger.model.User;
import cozychat.xlebnick.com.cmessenger.utils.ServerHelper;
import cozychat.xlebnick.com.cmessenger.utils.Utils;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class UserListFragment extends LoadableFragment {

    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private SmackConnection connection;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connection = SmackConnection.getInstance(getContext());
        connection.init(Utils.getPhoneNumber(getContext()),
                Utils.getPhoneNumber(getContext()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            UserAdapter adapter = new UserAdapter(mListener);
            recyclerView.setAdapter(adapter);
        }
        return super.onCreateView(inflater, container, savedInstanceState, view);
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Контакты");
        } catch (Exception ignored){}
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
            @Override
            protected Void doInBackground(Void... params) {
//                ServerHelper serverHelper = ServerHelper.getInstance(getContext(), null);
//                serverHelper.loginOrRegister();
//                usrs = serverHelper.getAllUsers();
                usrs = connection.getAllUsers();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (usrs != null){
                    onLoaded();
                    ((UserAdapter) recyclerView.getAdapter()).setValues(usrs);
                } else {
                    onFailed();
                }
                super.onPostExecute(aVoid);
            }
        }.execute();
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
