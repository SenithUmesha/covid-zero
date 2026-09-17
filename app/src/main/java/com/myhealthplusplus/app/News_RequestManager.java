package com.myhealthplusplus.app;

import android.content.Context;

import com.myhealthplusplus.app.Models.NewsApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class News_RequestManager {

    private final Context context;

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://newsapi.org/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public News_RequestManager(Context context) {
        this.context = context;
    }

    public void getNewsHeadlines(News_OnFetchDataListener listener, String sortBy) {
        if (BuildConfig.NEWS_API_KEY == null || BuildConfig.NEWS_API_KEY.trim().isEmpty()) {
            listener.onError("NewsAPI key is not configured. Add NEWS_API_KEY to secrets.properties.");
            return;
        }

        CallNewsApi callNewsApi = retrofit.create(CallNewsApi.class);
        Call<NewsApiResponse> call = callNewsApi.callHeadlines(
                "covid",
                sortBy,
                "en",
                BuildConfig.NEWS_API_KEY
        );

        call.enqueue(new Callback<NewsApiResponse>() {
            @Override
            public void onResponse(Call<NewsApiResponse> call, Response<NewsApiResponse> response) {
                NewsApiResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getArticles() == null) {
                    listener.onError("News request failed (HTTP " + response.code() + ").");
                    return;
                }

                listener.onFetchData(body.getArticles(), response.message());
            }

            @Override
            public void onFailure(Call<NewsApiResponse> call, Throwable t) {
                listener.onError("News request failed: " + t.getMessage());
            }
        });
    }

    public interface CallNewsApi {
        @GET("top-headlines")
        Call<NewsApiResponse> callHeadlines(
                @Query("q") String query,
                @Query("sortBy") String sortBy,
                @Query("language") String language,
                @Query("apiKey") String apiKey
        );
    }
}
