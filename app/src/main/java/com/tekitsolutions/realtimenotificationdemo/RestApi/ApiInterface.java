package com.tekitsolutions.realtimenotificationdemo.RestApi;

import com.tekitsolutions.realtimenotificationdemo.Model.PlaceList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import static com.tekitsolutions.realtimenotificationdemo.RestApi.ApiClient.HEADER;

public interface ApiInterface {

    @Headers(HEADER)
    @GET("json")
    Call<PlaceList> getDestination(@Query("location") String location, @Query("radius") int radius, @Query("key") String key);

}





