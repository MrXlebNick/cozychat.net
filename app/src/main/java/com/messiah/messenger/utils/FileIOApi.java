package com.messiah.messenger.utils;


import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/**
 * Created by AKiniyalocts on 2/23/15.
 * <p/>
 * This is our imgur API. It generates a rest API via Retrofit from Square inc.
 * <p/>
 * more here: http://square.github.io/retrofit/
 */
public interface FileIOApi {
    @Multipart
    @PUT("/upload") Call<FileResponse> upload(
                @Part MultipartBody.Part file
        );

    @Streaming
    @GET("/{key}")
    Call<ResponseBody> getFile(@Path("key") String key);

}
