package com.messiah.messenger.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.gson.Gson;
import com.messiah.messenger.Constants;
import com.messiah.messenger.R;
import com.messiah.messenger.helpers.DocumentHelper;
import com.messiah.messenger.helpers.ServerHelper;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.utils.FileIOApi;
import com.messiah.messenger.utils.FileResponse;
import com.messiah.messenger.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import xdroid.toaster.Toaster;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> implements Observer {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_FILE = 1;
    private List<Message> mValues;
    private Context mContext;
    private String opponent;
    private FileIOApi retrofit;

    public MessageAdapter(Context context) {
//        ServerHelper.getInstance(context).setFileListener(this);
        ServerHelper.getInstance(context).addObserver(this);
        mContext = context;
        retrofit = new Retrofit.Builder().baseUrl("http://ec2-34-208-141-31.us-west-2.compute.amazonaws.com:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(FileIOApi.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_FILE){

            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_message_file, parent, false);
            return new FileViewHolder(view);
        }
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        RelativeLayout.LayoutParams lp;
        lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, R.id.author);

        viewHolder.setItem(mValues.get(position));
        Log.d("***", "Sdcscsd" + new Gson().toJson(viewHolder.mItem));
        if (getItemViewType(position) == TYPE_MESSAGE){

            MessageViewHolder holder = (MessageViewHolder) viewHolder;

            if (holder.mItem.isFromMe) {

                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.mMessageView.setLayoutParams(lp);
                holder.mMessageView.setBackgroundResource(R.drawable.message_bubble);
            } else {

                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                holder.mMessageView.setLayoutParams(lp);
                holder.mMessageView.setBackgroundResource(R.drawable.message_bubble_opponent);
            }

            holder.mMessageView.setText(mValues.get(position).body);
        } else {
            FileViewHolder holder = (FileViewHolder) viewHolder;

            lp = (RelativeLayout.LayoutParams) holder.mFileView.getLayoutParams();
            float density = mContext.getResources().getDisplayMetrics().density;
            lp.width = (int) (120 * density);
            lp.height = (int) (120 * density);
            if (holder.mItem.isFromMe) {

                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                }
                holder.mFileView.setLayoutParams(lp);
                holder.mFileView.setBackgroundResource(R.drawable.message_bubble);
            } else {

                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                }
                holder.mFileView.setLayoutParams(lp);
                holder.mFileView.setBackgroundResource(R.drawable.message_bubble_opponent);
            }

//            Picasso.with(mContext)
//                    .load(R.drawable.ic_insert_drive_file_white_48dp)
//                    .into(holder.mFileView);
        }

        if (viewHolder.mItem.isFromMe) {
//            holder.mFileView.setBackgroundResource(R.drawable.message_bubble_me);

            viewHolder.mAuthorView.setText(R.string.you);

            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            viewHolder.mAuthorView.setLayoutParams(lp);


            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.LEFT_OF, R.id.message);
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.message);
            viewHolder.mDateView.setLayoutParams(lp);

        } else {


            viewHolder.mAuthorView.setText(opponent);

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


        if (getItemViewType(position) == TYPE_FILE){
            if (viewHolder.mItem.fileStatus.equals(Message.FILE_STATUS_LOADED)){

                ((FileViewHolder) viewHolder).mBytesView.setVisibility(View.VISIBLE);
                ((FileViewHolder) viewHolder).mBytesView.setText(((FileViewHolder) viewHolder).mItem.filePath.substring(((FileViewHolder) viewHolder).mItem.filePath.lastIndexOf("/") + 1));
            } else {
                ((FileViewHolder) viewHolder).mBytesView.setVisibility(View.GONE);
            }
            try {

                if (viewHolder.mItem.fileStatus.equals(Message.FILE_STATUS_LOADED)
                        && !TextUtils.isEmpty(viewHolder.mItem.filePath)
                        && Utils.getMimeType(viewHolder.mItem.filePath).contains("image")){
                    Picasso.with(mContext)
                            .load(viewHolder.mItem.fileUri)
                            .into(((FileViewHolder) viewHolder).mFileView);
                } else {
                    Picasso.with(mContext)
                            .load(R.drawable.ic_insert_drive_file_white_48dp)
                            .into(((FileViewHolder) viewHolder).mFileView);
                }
            } catch (Exception e){

                e.printStackTrace();
                Picasso.with(mContext)
                        .load(R.drawable.ic_insert_drive_file_white_48dp)
                        .into(((FileViewHolder) viewHolder).mFileView);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mValues.get(position).type.equals(Constants.MESSAGE_TYPE_FILE)) {
            return TYPE_FILE;
        }
        return TYPE_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return mValues == null ? 0 : mValues.size();
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
        notifyItemRangeChanged(0, mValues.size());
    }

    @Override
    public void update(Observable o, Object arg) {

//        notifyDataSetChanged();
//        notifyItemRangeChanged(0, getItemCount()-1);
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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

        public void setItem(Message item) {
            this.mItem = item;

        }
    }

    private class MessageViewHolder extends ViewHolder {
        final TextView mMessageView;

        MessageViewHolder(View view) {
            super(view);
            mMessageView = (TextView) view.findViewById(R.id.message);
        }

    }


    public class FileViewHolder extends ViewHolder{
        final ImageView mFileView;
        final AppCompatImageView mActionView;
        final CircularProgressView mStatusView;
        final TextView mBytesView;
        final TextView mNameView;
        Call call;
        Callback callback;


        public FileViewHolder(View view) {
            super(view);
            mFileView = (ImageView) view.findViewById(R.id.message);
            mActionView = (AppCompatImageView) view.findViewById(R.id.action);
            mStatusView = (CircularProgressView) view.findViewById(R.id.loading_status);
            mBytesView = (TextView) view.findViewById(R.id.bytes);
            mNameView = (TextView) view.findViewById(R.id.name);
//            refresh();


            mActionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("***", mItem.fileStatus);
                    switch (mItem.fileStatus) {
                        case Message.FILE_STATUS_LOADING:
                            if (call != null) {
                                call.cancel();
                                setStatus(Message.FILE_STATUS_FAILED);
                            }
                            break;
                        case Message.FILE_STATUS_FAILED:
                            call = call.clone();
                            call.enqueue(callback);
                            setStatus(Message.FILE_STATUS_LOADING);
                            //TODO: enqueue
                            break;
                        case Message.FILE_STATUS_LOADED:
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(mItem.fileUri), Utils.getMimeType(mItem.filePath));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                mContext.startActivity(intent);
                            } catch (Exception e) {
                                Toaster.toast(mContext.getString(R.string.error_in_opening_file) + e.getMessage());
                            }
                            break;
                    }
                }
            });
        }

        public void setItem(Message message){
            mItem = message;

            if (mItem.isFromMe){
                callback = new Callback<FileResponse>() {
                    @Override
                    public void onResponse(Call<FileResponse> call, Response<FileResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.isSuccessful()){
                                Log.d("***", "success");
                                setStatus(Message.FILE_STATUS_LOADED);
                                ServerHelper.getInstance(mContext).sendFileMessage(mItem, response.body().key +
                                        Constants.MESSAGE_FILE_INDEX_PREFIX + mItem.fileName);
                                return;
                            }
                        }

                        Toaster.toast("An error during sending file " + mItem.fileName );
                        Log.d("***", "onSuccess " + new Gson().toJson(response.errorBody()));
                        setStatus(Message.FILE_STATUS_FAILED);

                    }

                    @Override
                    public void onFailure(Call<FileResponse> call, Throwable t) {

                        Toaster.toast("An error during sending file " + mItem.fileName );
                        setStatus(Message.FILE_STATUS_FAILED);

                        t.printStackTrace();
                    }
                };

                final File file = new File(mItem.filePath);

                RequestBody requestFile;
                try {

                    String extension = MimeTypeMap.getFileExtensionFromUrl(mItem.filePath);
                    requestFile = RequestBody.create(
                            MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)),
                            file);
                } catch (Exception e){
                    requestFile = RequestBody.create(
                            MediaType.parse(mContext.getContentResolver().getType(Uri.parse(mItem.fileUri))),
                            file);

                }

                MultipartBody.Part body =
                        MultipartBody.Part.createFormData(Constants.FILE_MULTIPART_NAME, file.getName(), requestFile);


                call = retrofit.upload(body);
            } else {
                callback = new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                protected Boolean doInBackground(Void... voids) {
                                    return DocumentHelper.writeResponseBodyToDisk(mItem, response.body(), mItem.fileName);
                                }

                                @Override
                                protected void onPostExecute(Boolean writtenToDisk) {
                                    super.onPostExecute(writtenToDisk);

                                    if (writtenToDisk)
                                        setStatus(Message.FILE_STATUS_LOADED);
                                    else{
                                        Log.d("***", "on disk failure");
                                        setStatus(Message.FILE_STATUS_FAILED);
                                    }
                                }
                            }.execute();
                        } else {
                            Log.d("***", "on response failure ");
                            setStatus(Message.FILE_STATUS_FAILED);
                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        setStatus(Message.FILE_STATUS_FAILED);
                        t.printStackTrace();
                    }
                };
                call = retrofit.getFile(mItem.body);
            }

            if (!mItem.fileStatus.equals(Message.FILE_STATUS_LOADED) &&
                    !mItem.fileStatus.equals(Message.FILE_STATUS_FAILED)){

                call.enqueue(callback);
                setStatus(Message.FILE_STATUS_LOADING);
            }
            refresh();
        }

        private void refresh(){
            mStatusView.setMaxProgress(100);
            mStatusView.setIndeterminate(true);
            mStatusView.setProgress(50);

            switch (mItem.fileStatus) {
                case Message.FILE_STATUS_LOADING:
                    mStatusView.setVisibility(View.VISIBLE);
                    if (mItem.isFromMe) {
                        mActionView.setImageResource(R.drawable.ic_close_grey_600_24dp);
                    } else {
                        mActionView.setImageResource(R.drawable.ic_file_download_grey_600_24dp);
                    }

                    break;
                case Message.FILE_STATUS_FAILED:
                    mStatusView.setVisibility(View.GONE);
                    mActionView.setImageResource(R.drawable.ic_reload);

                    break;
                case Message.FILE_STATUS_LOADED:
                    mStatusView.setVisibility(View.GONE);
                    mBytesView.setVisibility(View.VISIBLE);
                    mBytesView.setText(mItem.filePath.substring(mItem.filePath.lastIndexOf("/")));
                    try {
                        if (!TextUtils.isEmpty(mItem.fileUri) && Utils.getMimeType(mItem.filePath).contains("image")){

                            Log.d("dsvjnskdjfmv", Utils.getMimeType(mItem.filePath));
                            Picasso.with(mContext)
                                    .load(mItem.fileUri)
                                    .into((ImageView) mView.findViewById(R.id.message));
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    Log.d("file file", "file" + mItem.filePath);
                    mActionView.setImageResource(R.drawable.ic_done_grey_600_24dp);
                    break;
            }

        }

        private void setStatus(String status){
            Log.d("newstatus", status);
            mItem.fileStatus = status;
            mItem.update();
            refresh();
        }


//        public void setFileTransfer(final FileTransfer fileTransfer) {
//            if (fileTransfer == null) {
//                return;
//            }
//            if (transfer == null){
//                transfer = fileTransfer;
//            }
//            mActionView.setImageResource(R.drawable.ic_close_red_900_48dp);
//
//            new Thread(new Runnable() {
//                int progress = 0 ;
//                boolean hasStarted = false;
//                @Override
//                public void run() {
//                    do {
//                        if (!hasStarted && fileTransfer.getStatus().equals(FileTransfer.Status.in_progress)) {
//                            ((Activity) mContext).runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mStatusView.setIndeterminate(false);
//                                    mStatusView.setVisibility(View.VISIBLE);
//                                }
//                            });
//                            hasStarted = true;
//                            mItem.fileStatus = Message.FILE_STATUS_LOADING;
//                        }
//
//                        if ((int) (fileTransfer.getProgress() * 100) != progress) {
//                            progress = ((int) (fileTransfer.getProgress() * 100));
//                            ((Activity) mContext).runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mStatusView.setProgress(progress);
//                                    mStatusView.setVisibility(View.VISIBLE);
//
//                                }
//                            });
//                        }
//
//                    } while (!fileTransfer.isDone());
//
//                    if ((fileTransfer.getError() == null ||
//                            fileTransfer.getError().equals(FileTransfer.Error.none)) &&
//                            fileTransfer.getStatus().equals(FileTransfer.Status.complete) &&
//                            fileTransfer.getProgress() == 1){
//                        ((Activity) mContext).runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mStatusView.setVisibility(GONE);
//                                mActionView.setImageResource(R.drawable.ic_done_grey_600_24dp);
//
//                            }
//                        });
//                        hasStarted = true;
//                    } else{
//                        fileTransfer.cancel();
//
//                        ((Activity) mContext).runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (mItem.isFromMe)
//                                mActionView.setImageResource(R.drawable.ic_autorenew_grey_800_24dp);
//                                mStatusView.setVisibility(GONE);
//
//                            }
//                        });
//
//                    }
//                }
//            }).start();
//
//
//        }


    }


}
