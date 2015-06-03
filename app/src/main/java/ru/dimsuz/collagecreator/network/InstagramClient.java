package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;


import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.util.NetworkUtils;
import ru.dimsuz.collagecreator.util.RxUtils;
import rx.Observable;
import timber.log.Timber;

public class InstagramClient {
    /**
     * A client id for instagram api, registered for this app
     */
    private final static String CLIENT_ID = "822685a536c4426b8d3fad3f03b84af6";
    private final OkHttpClient client;
    private final Gson gson;

    public InstagramClient(OkHttpClient client) {
        this.client = client;
        gson = new Gson();
    }

    public Observable<ImageInfo> getUserImages(UserInfo userInfo) {
        if(!userInfo.isValid()) {
            return Observable.error(new RuntimeException("passed user info is not valid: "+userInfo));
        }
        return null;
    }

    private static Observable<ImageInfo> fetchAllUserImages(OkHttpClient client, String userId) {
        return null;
    }

    }

    /**
     * Given a user name returns an instagram user id
     */
    public Observable<UserInfo> getUserInfo(final String userName) {
        if(userName == null || userName.isEmpty()) {
            return Observable.error(new IllegalArgumentException("username must not be empty"));
        }
        Timber.d("getting user info for: %s", userName);
        // count=1 is not enough, best match is not always first! (dunno why)
        String url = "https://api.instagram.com/v1/users/search?q=" + userName + "&count=20";
        return RxUtils
                .httpGetRequest(client, instagramUrl(url))
                .map(JsonParsers.parseUserInfo(gson, userName));
    }

    /**
     * Appends some common required parameters to the original url (client_id)
     */
    private static String instagramUrl(String url) {
        String encodedUrl = NetworkUtils.encodeUrl(url);
        if(!encodedUrl.contains("client_id")) {
            return encodedUrl.contains("?")
                    ? encodedUrl + "&client_id=" + CLIENT_ID
                    : encodedUrl + "?client_id=" + CLIENT_ID;
        } else {
            return encodedUrl;
        }
    }
}
