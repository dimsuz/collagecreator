package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import ru.dimsuz.collagecreator.data.Consts;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.util.NetworkUtils;
import ru.dimsuz.collagecreator.util.RxUtils;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
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
                        return JsonParsers.parseSearchResultsForMatch(response, gson, userName);
                    }
                });
    }

    /**
     * Returns <b>all</b> user images, sorted by likes count DESC
     */
    public Observable<List<ImageInfo>> getUserImages(final UserInfo userInfo) {
        if(userInfo == null || !userInfo.isValid()) {
            return Observable.error(new IllegalArgumentException("passed user info is not valid: "+userInfo));
        }
        // Retrieve a page after page until en empty data set is returned.
        // Subject here is used as the source of 'fetch next page' requests.
        // Whenever such request is emitted, next page is retrieved, parsed and appended to the whole
        // and finally when subjects emits onCompleted(), whole list is assembled and returned to the user
        //
        // NOTE: doing this iteratively. I first tried recursion (which doesn't need subject), but
        // when user has more than N pages it crashes, where N is amount needed for stack to overflow
        final BehaviorSubject<String> nextMaxIdEvents = BehaviorSubject.create("");
        return nextMaxIdEvents
                .flatMap(new Func1<String, Observable<ImageInfo>>() {
                    @Override
                    public Observable<ImageInfo> call(String maxId) {
                        return getImagesSinceMaxId(userInfo.userId(), maxId)
                                .doOnNext(requestNextPageOrFinish(nextMaxIdEvents))
                                // unroll to be recomposed to list later
                                .concatMap(new Func1<List<ImageInfo>, Observable<ImageInfo>>() {
                                    @Override
                                    public Observable<ImageInfo> call(List<ImageInfo> images) {
                                        return Observable.from(images);
                                    }
                                });
                    }
                })
                .toSortedList(createImageSortFunction());
    }

    private Observable<List<ImageInfo>> getImagesSinceMaxId(final String userId, @Nullable String maxId) {
        String url = "https://api.instagram.com/v1/users/" + userId + "/media/recent/"+
                "?count="+ Consts.IMAGES_PER_REQUEST_COUNT +
                (maxId != null && !maxId.isEmpty() ? "&max_id=" + maxId : "");
        return RxUtils.httpGetRequest(client, instagramUrl(url))
                // parse this page
                .map(new Func1<Response, List<ImageInfo>>() {
                    @Override
                    public List<ImageInfo> call(Response response) {
                        return JsonParsers.parseImagesResponse(response);
                    }
                });
    }

    @NotNull
    private static Action1<List<ImageInfo>> requestNextPageOrFinish(final BehaviorSubject<String> nextMaxIdEvents) {
        return new Action1<List<ImageInfo>>() {
            @Override
            public void call(List<ImageInfo> images) {
                if(images.isEmpty()) {
                    Timber.d("page load finished, no more data");
                    nextMaxIdEvents.onCompleted();
                } else {
                    Timber.d("loaded page, asking for next page");
                    ImageInfo curPageLastImage = images.get(images.size() - 1);
                    nextMaxIdEvents.onNext(curPageLastImage.id());
                }
            }
        };
    }

    @NotNull
    private static Func2<ImageInfo, ImageInfo, Integer> createImageSortFunction() {
        return new Func2<ImageInfo, ImageInfo, Integer>() {
            @Override
            public Integer call(ImageInfo imageInfo1, ImageInfo imageInfo2) {
                // need to be a little weirder than Integer.compare() to support earlier api levels
                return Integer.valueOf(imageInfo2.likesCount()).compareTo(imageInfo1.likesCount());
            }
        };
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
