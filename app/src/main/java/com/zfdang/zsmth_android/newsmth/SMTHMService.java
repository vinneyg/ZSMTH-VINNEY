package com.zfdang.zsmth_android.newsmth;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import io.reactivex.Observable;

/**
 * Created by Vinney on 2025-04-17.
 */

public interface SMTHMService {
    @Headers("X-Requested-With:XMLHttpRequest")
    @GET("board/{boardEngName}/0")
    Observable<ResponseBody> getBoardTopicsByPage(@Path("boardEngName") String boardEngName, @Query("p") String page);

    @Headers("X-Requested-With:XMLHttpRequest")
    @GET("/article/{boardEngName}/single/{topicID}/0")
    Call<ResponseBody> getPostListByPage(@Path("boardEngName") String boardEngName, @Path("topicID") String topicID);

    @Headers("X-Requested-With:XMLHttpRequest")
    @GET("/article/{boardEngName}/delete/{postID}")
    Observable<Response<ResponseBody>>  deletePostMobile(@Path("boardEngName") String boardEngName, @Path("postID") String postID,
                                                         @Query("s") int s);

}

