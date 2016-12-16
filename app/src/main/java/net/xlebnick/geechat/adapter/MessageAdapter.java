package net.xlebnick.geechat.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.squareup.picasso.Picasso;

import net.xlebnick.geechat.BitmapTransform;
import net.xlebnick.geechat.R;
import net.xlebnick.geechat.model.Message;
import net.xlebnick.geechat.utils.ServerHelper;
import net.xlebnick.geechat.utils.Utils;

import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import xdroid.toaster.Toaster;

import static android.view.View.GONE;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> implements  Observer {

    private List<Message> mValues;

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_FILE = 1;

    private Context mContext;
    public MessageAdapter(Context context){
//        ServerHelper.getInstance(context).setFileListener(this);
        ServerHelper.getInstance(context).addObserver(this);
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_MESSAGE){

            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_message, parent, false);
            return new MessageViewHolder(view);

        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_message_file, parent, false);

            return new FileViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        RelativeLayout.LayoutParams lp;
        lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, R.id.author);

        if (viewHolder instanceof MessageViewHolder){
            MessageViewHolder holder = (MessageViewHolder) viewHolder;
            holder.mItem = mValues.get(position);

            if (holder.mItem.isFromMe){

                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.mMessageView.setLayoutParams(lp);
                holder.mMessageView.setBackgroundResource(R.drawable.message_bubble);
            } else {

                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                holder.mMessageView.setLayoutParams(lp);
                holder.mMessageView.setBackgroundResource(R.drawable.message_bubble_opponent);
            }

            holder.mMessageView.setText(mValues.get(position).body);
        } else if (viewHolder instanceof FileViewHolder){
            final FileViewHolder holder = (FileViewHolder) viewHolder;
            holder.mItem = mValues.get(position);

            if (holder.mItem.type.equals(Message.TYPE_FILE)){
                switch (holder.mItem.fileStatus){
                    case Message.FILE_STATUS_PENDING:
                        holder.mStatusView.setVisibility(View.VISIBLE);
                        holder.mItem.fileStatus = Message.FILE_STATUS_LOADING;
                        holder.mActionView.setImageResource(R.drawable.ic_close_red_900_48dp);
                        holder.mItem.update();
                        if (holder.mItem.isFromMe){

                            OutgoingFileTransfer fileTransfer = ServerHelper.getInstance(mContext).sendFile(holder.mItem);
                            holder.setFileTransfer(fileTransfer);
                        } else {
                            holder.setFileTransfer(ServerHelper.getInstance(mContext).getIncomingFileTrnsfer(holder.mItem.messageId));
                        }

                        break;
                    case Message.FILE_STATUS_LOADED:
                        holder.mStatusView.setVisibility(GONE);
                        holder.mActionView.setImageResource(R.drawable.ic_done_grey_600_24dp);

//                        Glide.with(mContext).load(Uri.fromFile(new File(holder.mItem.body))).listener(new  RequestListener<Uri, GlideDrawable>(){
//                            @Override
//                            public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
//                                Glide.with(mContext).load(R.drawable.ic_insert_drive_file_white_48dp).listener(new RequestListener<Integer, GlideDrawable>() {
//                                    @Override
//                                    public boolean onException(Exception e, Integer model, Target<GlideDrawable> target, boolean isFirstResource) {
//                                        e.printStackTrace();
//                                        Log.d("***", "fail again");
//                                        return false;
//                                    }
//
//                                    @Override
//                                    public boolean onResourceReady(GlideDrawable resource, Integer model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                        Log.d("***", "not fail again");
//                                        return false;
//                                    }
//                                }).into(holder.mFileView);
//                                return true;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                Log.d("***", "works");
//                                return false;
//                            }
//                        }).into(holder.mFileView);
                        break;
                    case Message.FILE_STATUS_FAILED:
                        holder.mItem.fileStatus = Message.FILE_STATUS_FAILED;
                        holder.mItem.update();
                        holder.mStatusView.setVisibility(GONE);
                        holder.mActionView.setImageResource(R.drawable.ic_autorenew_grey_800_24dp);
                        break;
                    case Message.FILE_STATUS_LOADING:
                        holder.setFileTransfer(ServerHelper.getInstance(mContext).getIncomingFileTrnsfer(holder.mItem.messageId));
                        break;

                }
                Drawable drawable = Drawable.createFromPath(holder.mItem.body);
                if (drawable != null){

                    int size = (int) Math.ceil(Math.sqrt(384 * 512));
                    Picasso.with(mContext).load(new File(holder.mItem.body))
                            .transform(new BitmapTransform(384, 512))
                            .resize(size, size)
                            .centerInside().into(holder.mFileView);
                } else {
                    holder.mFileView.setImageResource(R.drawable.ic_insert_drive_file_white_48dp);
                }

            }

//            if (holder.mItem.fileStatus.equals(Message.FILE_STATUS_LOADING)
//                    || holder.mItem.fileStatus.equals(Message.FILE_STATUS_PENDING)){
//                for (Loading loading : loadings){
//                    if (loading.fileName.equals(holder.mItem.body)){
//
//                        holder.mBytesView.setText(loading.bytesSent  + "");
//                        holder.mStatusView.setText(loading.status);
//                    }
//                }
//            }
            if (holder.mItem.isFromMe){
//            holder.mFileView.setBackgroundResource(R.drawable.message_bubble_me);
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.mFileView.setLayoutParams(lp);
                holder.mFileView.setBackgroundResource(R.drawable.message_bubble);

            } else {

                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                holder.mFileView.setLayoutParams(lp);
                holder.mFileView.setBackgroundResource(R.drawable.message_bubble_opponent);

            }

        }

        if (viewHolder.mItem.isFromMe){
//            holder.mFileView.setBackgroundResource(R.drawable.message_bubble_me);

            viewHolder.mAuthorView.setText("You");

            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            viewHolder.mAuthorView.setLayoutParams(lp);



            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.LEFT_OF, R.id.message);
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.message);
            viewHolder.mDateView.setLayoutParams(lp);

        } else {


            viewHolder.mAuthorView.setText(viewHolder.mItem.sender);

            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            viewHolder.mAuthorView.setLayoutParams(lp);


            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.RIGHT_OF, R.id.message);
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.message);
            viewHolder.mDateView.setLayoutParams(lp);
        }


        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm dd/MM/yy", Locale.getDefault());
        viewHolder.mDateView.setText(sdf.format(new Date(viewHolder.mItem.time)));



    }

    @Override
    public int getItemCount() {
        return mValues == null ? 0 : mValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (!TextUtils.isEmpty(mValues.get(position).type) && mValues.get(position).type.equals(Message.TYPE_FILE)){
            return TYPE_FILE;
        }
        return TYPE_MESSAGE;
    }

//    @Override
//    public void onProgress(String status, long bytesSent, double progress, String error, String fileName) {
//
//        if (TextUtils.isEmpty(error)){
//
//            for (Message message : mValues){
//                if (message.type.equals(Message.TYPE_FILE) && message.body.equals(fileName)){
//                    message.fileStatus = Message.FILE_STATUS_LOADING;
//                    return;
//                }
//            }
//            for (Loading existingLoading : loadings){
//                if (fileName.equals(existingLoading.fileName)){
//                    existingLoading.bytesSent = bytesSent;
//                    existingLoading.status = status;
//                    existingLoading.progress = progress;
//                    existingLoading.error = error;
//                    notifyItemRangeChanged(0, mValues.size());
//                    return;
//                }
//            }
//
//            Loading loading = new Loading(status, bytesSent, progress, error, fileName);
//            loadings.add(loading);
//        } else {
//            for (Message message : mValues){
//                if (message.type.equals(Message.TYPE_FILE) && message.body.equals(fileName)){
//                    message.fileStatus = Message.FILE_STATUS_FAILED;
//                    Toaster.toast("File sending failed, reason: " + error);
//                    return;
//                }
//            }
//        }
//    }

    public void setValues(List<Message> mValues) {
        this.mValues = mValues;
        notifyDataSetChanged();
        notifyItemRangeChanged(0, mValues.size() );
    }

    @Override
    public void update(Observable o, Object arg) {

//        notifyDataSetChanged();
//        notifyItemRangeChanged(0, getItemCount()-1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        final View mView;
        final TextView mDateView;
        final TextView mAuthorView;
        public Message mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mAuthorView = (TextView) view.findViewById(R.id.author);
            mDateView = (TextView) view.findViewById(R.id.time);
        }
    }

    private class MessageViewHolder extends ViewHolder {
        final TextView mMessageView;

        MessageViewHolder(View view) {
            super(view);
            mMessageView = (TextView) view.findViewById(R.id.message);
        }

    }


    public class FileViewHolder extends ViewHolder {
        final ImageView mFileView;
        final AppCompatImageView mActionView;
        final CircularProgressView mStatusView;
        final TextView mBytesView;
        FileTransfer transfer;


        public FileViewHolder(View view) {
            super(view);
            mFileView = (ImageView) view.findViewById(R.id.message);
            mActionView = (AppCompatImageView) view.findViewById(R.id.action);
            mStatusView = (CircularProgressView) view.findViewById(R.id.loading_status);
            mStatusView.setMaxProgress(100);
            mStatusView.setIndeterminate(true);
            mStatusView.setProgress(0);
            mBytesView = (TextView) view.findViewById(R.id.bytes);
            mBytesView.setVisibility(GONE);

            mActionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("***", mItem.fileStatus);
                    if (mItem.fileStatus.equals(Message.FILE_STATUS_LOADING) || mItem.fileStatus.equals(Message.FILE_STATUS_PENDING) ){
                        if (transfer != null){
                            transfer.cancel();
                            if (mItem.isFromMe){
                                mActionView.setImageResource(R.drawable.ic_autorenew_grey_800_24dp);
                            } else {
                                mActionView.setImageResource(R.drawable.ic_close_grey_600_24dp);
                            }
                        }
                    } else if (mItem.fileStatus.equals(Message.FILE_STATUS_FAILED) && mItem.isFromMe){
                        OutgoingFileTransfer fileTransfer = ServerHelper.getInstance(mContext).sendFile(mItem);
                        setFileTransfer(fileTransfer);
                    } else if (mItem.fileStatus.equals(Message.FILE_STATUS_LOADED)){
                        try {
                            File file = new File(mItem.body);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(file), Utils.getMimeType(file.getAbsolutePath()));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            mContext.startActivity(intent);
                        } catch (Exception e){
                            Toaster.toast("An error during opening file, reason: " + e.getMessage());
                        }
                    }
                }
            });
        }


        public void setFileTransfer(final FileTransfer fileTransfer) {
            if (fileTransfer == null) {
                return;
            }
            if (transfer == null){
                transfer = fileTransfer;
            }
            mActionView.setImageResource(R.drawable.ic_close_red_900_48dp);

            new Thread(new Runnable() {
                int progress = 0 ;
                boolean hasStarted = false;
                @Override
                public void run() {
                    do {
                        if (!hasStarted && fileTransfer.getStatus().equals(FileTransfer.Status.in_progress)) {
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mStatusView.setIndeterminate(false);
                                    mStatusView.setVisibility(View.VISIBLE);
                                }
                            });
                            hasStarted = true;
                            mItem.fileStatus = Message.FILE_STATUS_LOADING;
                        }

                        if ((int) (fileTransfer.getProgress() * 100) != progress) {
                            progress = ((int) (fileTransfer.getProgress() * 100));
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mStatusView.setProgress(progress);
                                    Log.d("****", "progress is progressing to" + progress);
                                    mStatusView.setVisibility(View.VISIBLE);

                                }
                            });
                        }

                    } while (!fileTransfer.isDone());

                    if ((fileTransfer.getError() == null ||
                            fileTransfer.getError().equals(FileTransfer.Error.none)) &&
                            fileTransfer.getStatus().equals(FileTransfer.Status.complete) &&
                            fileTransfer.getProgress() == 1){
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mStatusView.setVisibility(GONE);
                                mActionView.setImageResource(R.drawable.ic_done_grey_600_24dp);

                            }
                        });
                        hasStarted = true;
                    } else{
                        fileTransfer.cancel();

                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mItem.isFromMe)
                                mActionView.setImageResource(R.drawable.ic_autorenew_grey_800_24dp);
                                mStatusView.setVisibility(GONE);

                            }
                        });

                    }
                }
            }).start();


        }
    }



}
