package ru.dimsuz.collagecreator.network;

import com.squareup.okhttp.OkHttpClient;

import ru.dimsuz.collagecreator.data.ImageInfo;
import rx.Observable;
import rx.functions.Func1;

public class InstagramApi {
    /**
     * A client id for instagram api, registered for this app
     */
    private final static String CLIENT_ID = "822685a536c4426b8d3fad3f03b84af6";
    private final OkHttpClient client;

    public InstagramApi(OkHttpClient client) {
        this.client = client;
    }

    public Observable<ImageInfo> getUserImages(String userName) {
        return getUserId(userName).flatMap(new Func1<String, Observable<ImageInfo>>() {
            @Override
            public Observable<ImageInfo> call(String userId) {
                // will cope with pagination and all that
                return fetchAllUserImages(client, userId);
            }
        });
    }

    private static Observable<ImageInfo> fetchAllUserImages(OkHttpClient client, String userId) {
        return null;
    }

    /**
     * Given a user name returns an instagram user id
     */
    private Observable<String> getUserId(String userName) {
        return Observable.error(new RuntimeException("no id"));
    }
}
