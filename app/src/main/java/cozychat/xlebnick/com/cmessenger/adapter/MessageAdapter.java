package cozychat.xlebnick.com.cmessenger.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.List;

import cozychat.xlebnick.com.cmessenger.R;
import cozychat.xlebnick.com.cmessenger.model.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> mValues;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        if (holder.mItem.isFromMe){
            holder.mMessageView.setBackgroundResource(R.drawable.message_bubble_me);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.mMessageView.getLayoutParams();
            lp.gravity = Gravity.RIGHT;
            holder.mMessageView.setLayoutParams(lp);
        } else {

            holder.mMessageView.setBackgroundResource(R.drawable.message_bubble_friend);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.mMessageView.getLayoutParams();
            lp.gravity = Gravity.LEFT;
            holder.mMessageView.setLayoutParams(lp);
        }
        holder.mMessageView.setText(mValues.get(position).body);
    }

    @Override
    public int getItemCount() {
        return mValues == null ? 0 : mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mMessageView;
        public Message mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mMessageView = (TextView) view.findViewById(R.id.message);
        }

    }

    public void setValues(List<Message> mValues) {
        this.mValues = mValues;
        notifyDataSetChanged();
    }
}
