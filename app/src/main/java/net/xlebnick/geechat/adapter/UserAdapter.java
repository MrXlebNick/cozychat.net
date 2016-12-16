package net.xlebnick.geechat.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import net.xlebnick.geechat.R;
import net.xlebnick.geechat.fragment.UserListFragment;
import net.xlebnick.geechat.model.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> mValues;
    private UserListFragment.OnListFragmentInteractionListener mListener;

    public UserAdapter(UserListFragment.OnListFragmentInteractionListener mListener) {
        this.mListener = mListener;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNameView.setText(TextUtils.isEmpty(mValues.get(position).mFullName)? "Secret Spy" : mValues.get(position).mFullName);
        holder.mPhoneView.setText(mValues.get(position).mPhoneNumber);
        holder.mMarkerView.setVisibility(mValues.get(position).hasUnread ? View.VISIBLE : View.GONE);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    public void setValues(List<User> mValues) {
        this.mValues = mValues;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mValues == null)
            return 0;
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mNameView;
        final TextView mPhoneView;
        final View mMarkerView;
        User mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.id);
            mPhoneView = (TextView) view.findViewById(R.id.content);
            mMarkerView = view.findViewById(R.id.marker);
        }

    }
}
