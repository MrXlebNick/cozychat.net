package com.messiah.messenger.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.messiah.messenger.R;
import com.messiah.messenger.helpers.DocumentHelper;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.utils.FileIOApi;
import com.messiah.messenger.utils.FileResponse;
import com.messiah.messenger.utils.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import xdroid.toaster.Toaster;

import static android.app.Activity.RESULT_OK;

public class ProfileInfoFragment extends LoadableFragment {

    private static final int SELECT_PICTURE = 2;
    private TextView nickNameView;
    private TextView phoneView;
    private EditText nameView;
    private EditText dateView;
    private EditText emailView;
    private EditText regionView;
    private EditText countryView;
    private EditText placeView;
    private EditText nativePlaceView;
    private EditText languagesView;
    private EditText education1View;
    private EditText education2View;
    private EditText websiteView;
    private EditText phone1View;
    private EditText phone2View;
    private EditText phone3View;
    private RadioGroup gender;
    private RadioButton male;
    private RadioButton female;
    private ImageView avatarView;

    private FileIOApi retrofit;

    private String avatarKey;
    private String avatarFileName;
    private boolean isAvatarUpdated = false;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile_info, container, false);

        retrofit = new Retrofit.Builder().baseUrl("http://ec2-35-162-177-84.us-west-2.compute.amazonaws.com:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(FileIOApi.class);

        avatarView = (ImageView) view.findViewById(R.id.avatar);
        avatarView.setOnClickListener(v -> {

            isAvatarUpdated = true;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.elect_pic)), SELECT_PICTURE);
        });

        nickNameView = (TextView) view.findViewById(R.id.nickname);
        phoneView = (TextView) view.findViewById(R.id.phone_number);
        nameView = (EditText) view.findViewById(R.id.name);
        dateView = (EditText) view.findViewById(R.id.date);
        emailView = (EditText) view.findViewById(R.id.email);
        regionView = (EditText) view.findViewById(R.id.region);
        countryView = (EditText) view.findViewById(R.id.country);
        placeView = (EditText) view.findViewById(R.id.place);
        nativePlaceView = (EditText) view.findViewById(R.id.native_place);
        languagesView = (EditText) view.findViewById(R.id.languages);
        education1View = (EditText) view.findViewById(R.id.education1);
        education2View = (EditText) view.findViewById(R.id.education2);
        websiteView = (EditText) view.findViewById(R.id.website);
        phone1View = (EditText) view.findViewById(R.id.phone1);
        phone2View = (EditText) view.findViewById(R.id.phone2);
        phone3View = (EditText) view.findViewById(R.id.phone3);
        gender = (RadioGroup) view.findViewById(R.id.gender);
        male = (RadioButton) view.findViewById(R.id.male);
        female = (RadioButton) view.findViewById(R.id.female);

        nickNameView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                return;
            ((TextView) v).setCursorVisible(false);
            ((TextView) v).setFocusableInTouchMode(false);
            ((TextView) v).setInputType(InputType.TYPE_NULL);
        });

        nickNameView.setOnClickListener(v -> {
            ((TextView) v).setCursorVisible(true);
            ((TextView) v).setFocusableInTouchMode(true);
            ((TextView) v).setInputType(InputType.TYPE_CLASS_TEXT);
            ((TextView) v).requestFocus(); //to trigger the soft input
        });


        View result = super.onCreateView(inflater, container, savedInstanceState, view);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {

                Uri returnUri = data.getData();
                String filePath = DocumentHelper.getPath(getContext(), returnUri);
                if (filePath == null || filePath.isEmpty()) return;

                Callback<FileResponse> callback = new Callback<FileResponse>() {
                    @Override
                    public void onResponse(Call<FileResponse> call, Response<FileResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.isSuccessful()){
                                Log.d("***", "success");
                                avatarKey = response.body().key;

                                onLoaded();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<FileResponse> call, Throwable t) {

                        Toaster.toast("An error during sending file " );
                        onLoaded();

                        t.printStackTrace();
                    }
                };

                getActivity().runOnUiThread(() -> Picasso.with(getContext())
                        .load(returnUri)
                        .fit()
                        .centerCrop()
                        .into(avatarView));
                avatarFileName = new File(filePath).getName();
                onLoadStart();

                XmppHelper.getInstance().uploadAvatar(new File(filePath), callback);

            }
        }
    }

    @Override
    public void onResume() {

        isAvatarUpdated = false;
        try {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(null);
        } catch (Exception ignored) {}
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile, menu);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (SipManager.isVoipSupported(getContext()) && SipManager.isApiSupported(getContext()) ){
//
//            Intent intent = new Intent(getContext(), DialActivity.class);
//            intent.putExtra("sip", mSipNumber);
//            startActivity(intent);
//        } else {
//            Toast.makeText(getContext(), "Your device does not support SIP stack, please wait for update", Toast.LENGTH_LONG).show();
//        }
        if (item.getItemId() == R.id.save){

            HashMap<String, String> properties = new HashMap<>();

            properties.put("niknam", nickNameView.getText().toString());
            properties.put("name", nameView.getText().toString());
            properties.put("date", dateView.getText().toString());
            properties.put("email", emailView.getText().toString());
            properties.put("region", regionView.getText().toString());
            properties.put("country", countryView.getText().toString());
            properties.put("place", placeView.getText().toString());
            properties.put("nativePlace", nativePlaceView.getText().toString());
            properties.put("languages", languagesView.getText().toString());
            properties.put("education1", education1View.getText().toString());
            properties.put("education2", education2View.getText().toString());
            properties.put("website", websiteView.getText().toString());
            properties.put("phone1", phone1View.getText().toString());
            properties.put("phone2", phone2View.getText().toString());
            properties.put("phone3", phone3View.getText().toString());
            if (male.isChecked()){
                properties.put("gender", "male");
            }
            if (female.isChecked()){
                properties.put("gender", "female");
            }

            if (!TextUtils.isEmpty(avatarKey)){
                properties.put("avatarKey", avatarKey);
            }
            if (!TextUtils.isEmpty(avatarFileName)){
                properties.put("avatarFileName", avatarFileName);
            }

            XmppHelper.getInstance().setUserPropertiesrx(properties).subscribe(
                    aBoolean -> Toaster.toast("Saved successfully"),
                    e -> {
                        e.printStackTrace();
                        Toaster.toast("An error occured, try again. Error: " + e.getMessage());
                    });
        }
        return true;
    }

    @Override
    protected void onLoadStart() {

        if (isAvatarUpdated){
            onLoaded();
            return;
        }
        XmppHelper
                .getInstance()
                .getUserPropertiesrx()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(properties -> {
            Log.d("profile", "number is " + Utils.getPhoneNumber(getContext()) + " " + properties.get("niknam"));
            nickNameView.setText(!TextUtils.isEmpty(properties.get("niknam")) ? properties.get("niknam") : "NickName");
                        phoneView.setText(!TextUtils.isEmpty( Utils.getPhoneNumber(getContext())) ? Utils.getPhoneNumber(getContext()) : "");
                        nameView.setText(!TextUtils.isEmpty(properties.get("name")) ? properties.get("name") : "");
                        dateView.setText(!TextUtils.isEmpty(properties.get("date")) ? properties.get("date") : "");
                        emailView.setText(!TextUtils.isEmpty(properties.get("email")) ? properties.get("email") : "");
                        regionView.setText(!TextUtils.isEmpty(properties.get("region")) ? properties.get("region") : "");
                        countryView.setText(!TextUtils.isEmpty(properties.get("country")) ? properties.get("country") : "");
                        placeView.setText(!TextUtils.isEmpty(properties.get("place")) ? properties.get("place") : "");
                        nativePlaceView.setText(!TextUtils.isEmpty(properties.get("nativePlace")) ? properties.get("nativePlace") : "");
                        languagesView.setText(!TextUtils.isEmpty(properties.get("languages")) ? properties.get("languages") : "");
                        education1View.setText(!TextUtils.isEmpty(properties.get("education1")) ? properties.get("education1") : "");
                        education2View.setText(!TextUtils.isEmpty(properties.get("education2")) ? properties.get("education2") : "");
                        websiteView.setText(!TextUtils.isEmpty(properties.get("website")) ? properties.get("website") : "");
                        phone1View.setText(!TextUtils.isEmpty(properties.get("phone1")) ? properties.get("phone1") : "");
                        phone2View.setText(!TextUtils.isEmpty(properties.get("phone2")) ? properties.get("phone2") : "");
                        phone3View.setText(!TextUtils.isEmpty(properties.get("phone3")) ? properties.get("phone3") : "");

                        if (!TextUtils.isEmpty(properties.get("avatarKey")) && !TextUtils.isEmpty(properties.get("avatarFileName"))){
                            Picasso.with(getContext())
                                    .load("http://ec2-35-162-177-84.us-west-2.compute.amazonaws.com:8080/" + properties.get("avatarKey"))
                                    .fit()
                                    .centerCrop()
                                    .into(avatarView);
//
//
//                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//                        XmppHelper.getInstance().downloadAvatarRx(properties.get("avatarKey"),properties.get("avatarFileName"))
//                                .subscribeOn(Schedulers.newThread())
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(file -> {
//                                            Log.d("***", file.getName());
//                                            Picasso.with(getContext())
//                                                    .load(file)
//                                                    .fit()
//                                                    .centerCrop()
//                                                    .into(avatarView);
//                                        },
//                                        throwable -> Toaster.toast("An error while loading image: " + throwable));

                    }
            nameView.setHint("Not specified");
            dateView.setHint("Not specified");
            emailView.setHint("Not specified");
            regionView.setHint("Not specified");
            countryView.setHint("Not specified");
            placeView.setHint("Not specified");
            nativePlaceView.setHint("Not specified");
            languagesView.setHint("Not specified");
            education1View.setHint("Not specified");
            education2View.setHint("Not specified");
            websiteView.setHint("Not specified");
            phone1View.setHint("Not specified");
            phone2View.setHint("Not specified");
            phone3View.setHint("Not specified");
            if (!TextUtils.isEmpty(properties.get("gender"))){
                male.setChecked(properties.get("gender").equals("male"));
                female.setChecked(!properties.get("gender").equals("male"));
            }
            onLoaded();
        }, throwable -> {
            throwable.printStackTrace();
            onEmpty(throwable.getMessage());
        });
    }

}
