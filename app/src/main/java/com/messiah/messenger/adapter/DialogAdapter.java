package com.messiah.messenger.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.fragment.UserListFragment;
import com.messiah.messenger.model.Dialog;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Dialog} and makes a call to the
 * specified {@link UserListFragment.OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class DialogAdapter extends RecyclerView.Adapter<DialogAdapter.ViewHolder> {

    private List<Dialog> mValues;
    private UserListFragment.OnListFragmentInteractionListener mListener;

    public DialogAdapter(UserListFragment.OnListFragmentInteractionListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mSenderView.setText(mValues.get(position).peer.mFullName);
        holder.mMessageView.setText(mValues.get(position).message);

        holder.mMarkerView.setText(mValues.get(position).count + "");
        holder.mMarkerView.setVisibility(mValues.get(position).count == 0 ? View.GONE : View.VISIBLE);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem.peer);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mValues == null)
            return 0;
        return mValues.size();
    }

    public void setValues(List<Dialog> mValues) {
        this.mValues = mValues;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final View mView;
        final TextView mSenderView;
        final TextView mMessageView;
        final TextView mMarkerView;
        public Dialog mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mSenderView = (TextView) view.findViewById(R.id.id);
            mMessageView = (TextView) view.findViewById(R.id.content);
            mMarkerView = (TextView) view.findViewById(R.id.marker);
        }
    }
}
