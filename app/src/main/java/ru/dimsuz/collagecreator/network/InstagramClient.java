package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.util.NetworkUtils;
import ru.dimsuz.collagecreator.util.RxUtils;
import rx.Observable;
import rx.functions.Func1;
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

    public Observable<List<ImageInfo>> getUserImages(UserInfo userInfo) {
        if(userInfo == null || !userInfo.isValid()) {
            return Observable.error(new IllegalArgumentException("passed user info is not valid: "+userInfo));
        }
        return getImagesSinceMaxId(userInfo.userId(), null);
    }

    private Observable<List<ImageInfo>> getImagesSinceMaxId(final String userId, @Nullable String maxId) {
        String url = "https://api.instagram.com/v1/users/" + userId + "/media/recent/"+
                "?count="+ Consts.IMAGES_PER_REQUEST_COUNT +
                (maxId != null ? "&max_id=" + maxId : "");
        return RxUtils.httpGetRequest(client, instagramUrl(url))
                // parse this page
                .map(new Func1<Response, List<ImageInfo>>() {
                    @Override
                    public List<ImageInfo> call(Response response) {
                        try {
                            return JsonParsers.parseImagesResponse(response);
                        } catch(IOException e) {
                            Timber.e(e, "failed to read image info json");
                            throw new RuntimeException(e);
                        }
                    }
                })
                // and then continue fetching recursively and joining results until no more data is returned
                .flatMap(new Func1<List<ImageInfo>, Observable<List<ImageInfo>>>() {
                    @Override
                    public Observable<List<ImageInfo>> call(final List<ImageInfo> curPageImages) {
                        if(curPageImages.isEmpty()) {
                            // finished, no more data to fetch...
                            return Observable.just(curPageImages);
                        } else {
                            // obtain next page data and then prepend the current list to it.
                            // this is recursion, bro
                            ImageInfo curPageLastId = curPageImages.get(curPageImages.size() - 1);
                            Timber.d("fetching next image page...");
                            return getImagesSinceMaxId(userId, curPageLastId.id())
                                    .map(new Func1<List<ImageInfo>, List<ImageInfo>>() {
                                        @Override
                                        public List<ImageInfo> call(List<ImageInfo> nextPageImages) {
                                            // watch out! not immutable, keep an eye on this one
                                            Timber.d("this page contained %d images", nextPageImages.size());
                                            curPageImages.addAll(nextPageImages);
                                            return curPageImages;
                                        }
                                    });
                        }
                    }
                });
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
                .map(new Func1<Response, UserInfo>() {
                    @Override
                    public UserInfo call(Response response) {
                        try {
                            return JsonParsers.parseUserInfo(response, gson, userName);
                        } catch(IOException e) {
                            Timber.e(e, "failed to read user info json");
                            throw new RuntimeException(e);
                        }
                    }
                });
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
