package ru.dimsuz.collagecreator.network;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import ru.dimsuz.collagecreator.util.RxUtils;
import rx.Observable;
import rx.functions.Func0;
import timber.log.Timber;

public class InstagramClient {
    /**
     * A client id for instagram api, registered for this app
     */
    private final static String CLIENT_ID = "822685a536c4426b8d3fad3f03b84af6";
    private final OkHttpClient client;

    public InstagramClient(OkHttpClient client) {
        this.client = client;
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

    /**
     * Given a user name returns an instagram user id
     */
    public Observable<UserInfo> getUserInfo(final String userName) {
        return RxUtils.runOnce(new Func0<UserInfo>() {
            @Override
            public UserInfo call() {
                Timber.d("getting user info for: %s", userName);
                if(userName.startsWith("ra") || userName.endsWith("ha")) {
                    return UserInfo.create(userName, "123");
                }
                throw new RuntimeException("invalid username");
            }
        }).delay(800, TimeUnit.MILLISECONDS);
    }
}
