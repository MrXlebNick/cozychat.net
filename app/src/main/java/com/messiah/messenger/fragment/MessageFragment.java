package com.messiah.messenger.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.sip.SipManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AndroidException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.messiah.messenger.Constants;
import com.messiah.messenger.R;
import com.messiah.messenger.activity.DialActivity;
import com.messiah.messenger.activity.MainActivity;
import com.messiah.messenger.adapter.MessageAdapter;
import com.messiah.messenger.helpers.DocumentHelper;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.model.Message;
import com.messiah.messenger.model.User;
import com.messiah.messenger.service.PjsipService;
import com.messiah.messenger.utils.Utils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import io.github.rockerhieu.emojicon.EmojiconEditText;
import io.github.rockerhieu.emojicon.EmojiconGridFragment;
import io.github.rockerhieu.emojicon.EmojiconGridView;
import io.github.rockerhieu.emojicon.emoji.Emojicon;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import xdroid.toaster.Toaster;

import static android.app.Activity.RESULT_OK;

public class MessageFragment extends LoadableFragment  {

    public final static int FILE_PICK = 1001;
    private static final int PICK_IMAGE = 1;
    private static final int PICK_FILE = 2;
    // TODO: Customize parameter argument names
    private static final String ARG_PHONE_NUMBER = "column-count";
    private static final String ARG_SIP_NUMBER = "sip";
    public static boolean isActive = false;
    BroadcastReceiver receiver;
    // TODO: Customize parameters
    private String mPhoneNumber;
    private String mSipNumber;
    private List<Message> messages;
    private RecyclerView recyclerView;
    private EmojiconEditText editText;
    private boolean isNewMesageEmpty = true;
    private MediaPlayer mediaPlayer;

    private ImageView btnEmoji;

    private String opponent;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageFragment() {}

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE) {

                Uri returnUri = data.getData();
                String filePath = DocumentHelper.getPath(getContext(), returnUri);
                if (filePath == null || filePath.isEmpty()) return;

//
//                Retrofit retrofit = new Retrofit.Builder()
//                        .baseUrl("https://file.io")
//                        .addConverterFactory(GsonConverterFactory.create())
//                        .build();
//                FileIOApi service =
//                        retrofit.create(FileIOApi.class);


                final Message message = new Message();
                message.isFromMe = true;
                message.type = Constants.MESSAGE_TYPE_FILE;
                message.time = System.currentTimeMillis();
                message.receiver = mPhoneNumber;
                message.sender = Utils.getPhoneNumber(getContext());
                message.messageId = System.currentTimeMillis() + "";
                message.filePath = filePath;
                message.fileUri = returnUri.toString();
                message.fileName = new File(filePath).getName();
                message.save();
                onLoadStart();
//
//                FileManager fileManager = new Retrofit.Builder().baseUrl("http://http://ec2-18-216-77-83.us-east-2.compute.amazonaws.com:8080")
//                        .build().create(FileManager.class);
//                File file = new File(filePath);
//
//                RequestBody requestFile =
//                        RequestBody.create(
//                                MediaType.parse(getContext().getContentResolver().getType(returnUri)),
//                                file
//                        );
//
//                // MultipartBody.Part is used to send also the actual file name
//                MultipartBody.Part body =
//                        MultipartBody.Part.createFormData("file", file.getName(), requestFile);
//
//                // add another part within the multipart request
//                String descriptionString = "hello, this is description speaking";
//                RequestBody description =
//                        RequestBody.create(
//                                okhttp3.MultipartBody.FORM, descriptionString);
//
//                // finally, execute the request
//                Call<ResponseBody> call = fileManager.upload(description, body);
//                call.enqueue(new Callback<ResponseBody>() {
//                    @Override
//                    public void onResponse(Call<ResponseBody> call,
//                                           Response<ResponseBody> response) {
//                        Log.v("Upload", "success");
//                    }
//
//                    @Override
//                    public void onFailure(Call<ResponseBody> call, Throwable t) {
//                        Log.e("Upload error:", t.getMessage());
//                    }
//                });

//                final long id = message.time;
//                Call<FileResponse> call = service.postFile(body);
//                call.enqueue(new Callback<FileResponse>() {
//                    @Override
//                    public void onResponse(Call<FileResponse> call, Response<FileResponse> response) {
//                        message.body = response.body().key;
//                        message.update();
//                        onLoadStart();
//                        XmppHelper.getInstance(getContext()).sendMessage(message);
//                    }
//
//                    @Override
//                    public void onFailure(Call<FileResponse> call, Throwable t) {
//                        Message updatingMessage = Message.find(Message.class, "message_id=?", String.valueOf(id)).get(0);
//                        updatingMessage.save();
//                        onLoadStart();
//                        t.printStackTrace();
//                    }
//                });


            } else if (requestCode == PICK_FILE) {


            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mediaPlayer = MediaPlayer.create(getContext(), R.raw.notif);
        final View view = inflater.inflate(R.layout.fragment_message_list, container, false);
        final Context context = view.getContext();
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, true));
        recyclerView.setAdapter(new MessageAdapter(getContext()));

        editText = (EmojiconEditText) view.findViewById(R.id.edit_text);

        btnEmoji = (ImageView) view.findViewById(R.id.btn_show_emoji);

        final EmojiconGridView emojiconsView = (EmojiconGridView) view.findViewById(R.id.emojicons_view);
        emojiconsView.setEmojiData(Emojicon.TYPE_PEOPLE, null, false);
        emojiconsView.setOnEmojiconClickedListener(emojicon -> {
            editText.setText(editText.getText() + emojicon.getEmoji());
            int position = editText.length();
            Editable etext = editText.getText();
            Selection.setSelection(etext, position);
        });

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        final int stageWidth = (int) (display.getWidth() / 1.25);
        btnEmoji.setOnClickListener(v -> {
            if (emojiconsView.getVisibility() == View.VISIBLE) {
                btnEmoji.setImageResource(R.drawable.ic_insert_emoticon_black_24dp);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                view.findViewById(R.id.new_message_block).setLayoutParams(lp);
                emojiconsView.setVisibility(View.GONE);
                (new Handler()).postDelayed(() -> {

                    editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                    int position = editText.length();
                    Editable etext = editText.getText();
                    Selection.setSelection(etext, position);

                }, 200);

            } else {
                btnEmoji.setImageResource(R.drawable.ic_keyboard_black_24dp);
                emojiconsView.setVisibility(View.VISIBLE);
                emojiconsView.getLayoutParams().width = stageWidth;

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.ABOVE, emojiconsView.getId());

                lp.bottomMargin = 67;
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                view.findViewById(R.id.new_message_block).setLayoutParams(lp);

            }


        });

        final ImageView btnSend = (ImageView) view.findViewById(R.id.btn_send);
        final CircularProgressView sendProgressBar = (CircularProgressView) view.findViewById(R.id.sending);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    isNewMesageEmpty = false;

                    btnSend.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));

                } else {
                    isNewMesageEmpty = true;
                    btnSend.setColorFilter(Color.GRAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        btnSend.setOnClickListener(v -> {
            if (isNewMesageEmpty) {
                Toaster.toast(R.string.enter_something_first);
                return;
            }

            btnSend.setVisibility(View.INVISIBLE);
            sendProgressBar.setVisibility(View.VISIBLE);
            sendProgressBar.setProgress(0);
            sendProgressBar.setIndeterminate(true);

            try {
                XmppHelper.getInstance(mPhoneNumber)
                        .sendMessagerx(mPhoneNumber,
                                URLEncoder.encode(editText.getText().toString(), "utf-8"))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(newMessage -> {

                                getActivity().runOnUiThread(() -> {
                                    messages.add(0, newMessage);
                                    ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                                });

                                mediaPlayer.start();
                            },
                            throwable -> {
                                throwable.printStackTrace();
                                Toaster.toast("Message was not sent: " + throwable.getMessage());
                                btnSend.setColorFilter(Color.GRAY);
                                btnSend.setVisibility(View.VISIBLE);
                                sendProgressBar.setVisibility(View.GONE);
                                editText.setText("");
                            }, () -> {
                                btnSend.setColorFilter(Color.GRAY);
                                btnSend.setVisibility(View.VISIBLE);
                                sendProgressBar.setVisibility(View.GONE);
                                editText.setText("");
                                recyclerView.scrollBy(5, 5);
                            });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            isNewMesageEmpty = true;
        });

        final View btnAttach = view.findViewById(R.id.btn_attach);
        btnAttach.setOnClickListener(v -> btnAttach.performLongClick());
        registerForContextMenu(btnAttach);

        return super.onCreateView(inflater, container, savedInstanceState, view);
    }


    private List<Message> getMessagesFromDb() {
        List<Message> messages =  Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE sender = ? OR receiver = ?  ORDER BY time DESC",
                mPhoneNumber, mPhoneNumber);

        return messages;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        isActive = false;
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.message_list_menu, menu);
//        menu.add(0, 1, 0, "Call");
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        Toaster.toast("Feature is under construction");
        if (true || SipManager.isVoipSupported(getContext()) && SipManager.isApiSupported(getContext())) {
            if (ContextCompat.checkSelfPermission(getContext(), "android.permission.USE_SIP") !=
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.RECORD_AUDIO) !=
                            PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(getActivity(), new String[]{"android.permission.USE_SIP", android.Manifest.permission.RECORD_AUDIO}, 1010);
                return true;
            }
//            if (mSipNumber == null || mSipNumber.isEmpty()){
//                Toaster.toast("This user is unreachable via call");
//                return true;
//            }

            PjsipService.call(mSipNumber);
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
        menu.add(0, v.getId(), 0, R.string.choose_from_gallery);
        menu.add(0, v.getId(), 1, R.string.choose_from_file);
    }

    public boolean onContextItemSelected(MenuItem item) {
//        Toaster.toast("This feature will be available soon");
//        return true;

        if (item.getOrder() == 0) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.elect_pic)), PICK_IMAGE);
        } else {
            DialogProperties properties = new DialogProperties();

            properties.selection_mode = DialogConfigs.SINGLE_MODE;
            properties.selection_type = DialogConfigs.FILE_SELECT;
            properties.root = new File(DialogConfigs.DEFAULT_DIR);
            properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
            properties.extensions = null;
            FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
            dialog.setTitle(getString(R.string.select_file));
            dialog.setDialogSelectionListener(files -> {
                for (String filePath : files) {
                    File file = new File(filePath);
                    Uri returnUri = Uri.fromFile(file);
                    if (filePath == null || filePath.isEmpty()) return;


                    final Message message = new Message();
                    message.isFromMe = true;
                    message.type = Constants.MESSAGE_TYPE_FILE;
                    message.time = System.currentTimeMillis();
                    message.receiver = mPhoneNumber;
                    message.sender = Utils.getPhoneNumber(getContext());
                    message.messageId = System.currentTimeMillis() + "";
                    message.filePath = filePath;
                    message.fileUri = returnUri.toString();
                    message.fileName = new File(filePath).getName();
                    message.save();
                    onLoadStart();
//
                }
            });
            dialog.show();
        }
        return true;
    }

    private void markAllAsRead() {
        List<Message> messages = Message.findWithQuery(Message.class,
                "SELECT * FROM Message WHERE (sender = ? OR receiver = ?) AND read = 0  ORDER BY time DESC",
                mPhoneNumber, mPhoneNumber);

        for (Message message : messages) {
            message.read = true;
            message.update();
        }

    }

    @Subscribe
    public void update(Message message) {

        Log.d("***", "DialogListFragment got the message");
        if (isActive) {
            getActivity().runOnUiThread(this::load);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        isActive = true;
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onLoadStart() {

        if (getArguments() != null) {
            mPhoneNumber = getArguments().getString(ARG_PHONE_NUMBER);
            mSipNumber = getArguments().getString(ARG_SIP_NUMBER);
            Log.d("sipka", mSipNumber + "1");

            XmppHelper.getInstance()
                    .getUserPropertiesrx(mPhoneNumber)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(properties -> {
                mSipNumber = properties.get("sip");
                Log.d("sipka", mSipNumber + "2");
                try {

                    List<User> user = User.find(User.class, "m_phone_number = ?", mPhoneNumber);
                    opponent = user.size() == 0 ? (TextUtils.isEmpty(properties.get("niknam")) ? "Secret Spy" : properties.get("niknam")) : user.get(0).mFullName;
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(opponent);

                    Picasso.with(getContext())
                            .load("http://ec2-18-216-77-83.us-east-2.compute.amazonaws.com:8080/" +
                                    properties.get("avatarKey"))
                            .fit()
                            .centerCrop()
                            .into((ImageView) getActivity().findViewById(R.id.avatar));

                } catch (Exception ignored) {}
            });


        }

        markAllAsRead();
        messages = getMessagesFromDb();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
                ((MessageAdapter) recyclerView.getAdapter()).setOpponent(opponent);
                recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount());
                recyclerView.scrollBy(5, 5);
                Utils.dismissNotifications(getContext());
            });

        }
//        receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
////
////                markAllAsRead();
////                messages = getMessagesFromDb();
////                ((MessageAdapter) recyclerView.getAdapter()).setValues(messages);
//
//            }
//        };
//        LocalBroadcastManager.getInstance(getContext()).registerReceiver((receiver),
//                new IntentFilter(XmppHelper.MESSAGE_RECEIVED)
//        );


        onLoaded();
    }

}
