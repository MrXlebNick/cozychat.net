package com.messiah.messenger.fragment;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

/**
 * Created by XlebNick for CMessenger.
 */

public interface FileManager {
    @Multipart
    @PUT("upload")
    Call<ResponseBody> upload(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part file
    );
}
