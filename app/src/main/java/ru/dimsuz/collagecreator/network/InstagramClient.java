package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Arrays;

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

    public Observable<ImageInfo> getUserImages(UserInfo userInfo) {
        if(!userInfo.isValid()) {
            return Observable.error(new RuntimeException("passed user info is not valid: "+userInfo));
        }
        return null;
    }

    private static Observable<ImageInfo> fetchAllUserImages(OkHttpClient client, String userId) {
        return null;
    }

    private static class UserResponseData {
        public String username;
        public String id;

        @Override
        public String toString() {
            return "UserResponseData{" +
                    "username='" + username + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    /**
     * Given a user name returns an instagram user id
     */
    public Observable<UserInfo> getUserInfo(final String userName) {
        if(userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
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
                            // FIXME prettify this stuff!
                            JsonReader jsonReader = new JsonReader(response.body().charStream());
                            jsonReader.beginObject();
                            while(jsonReader.hasNext()) {
                                String key = jsonReader.nextName();
                                if(key.equals("data")) {
                                    UserResponseData[] result = gson.fromJson(jsonReader, UserResponseData[].class);
                                    Timber.d("got this!\n%s", Arrays.asList(result));
                                    UserResponseData match = null;
                                    if(result.length != 0) {
                                        // for some reason data is returned in not sorted by best match order,
                                        // (for example 'fairylitte' query produces 'fairylittlefingers' first and 'fairylittle'
                                        // goes later!)
                                        // so need to search for the
                                        // exact match otherwise use first supplied!
                                        Timber.d("got %d matches for %s, searching for exact match", result.length, userName);
                                        for(int i = 0, sz = result.length; i < sz; i++) {
                                            if(result[i].username.equals(userName)) {
                                                match = result[i];
                                                break;
                                            }
                                        }
                                    }
                                    if(match != null) {
                                        return UserInfo.create(match.username, match.id);
                                    } else {
                                        throw new RuntimeException("username '" + userName + "' not found");
                                    }
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        } catch(IOException e) {
                            Timber.e("failed to read json");
                            throw new RuntimeException(e);
                        }
                        throw new RuntimeException("username '" + userName + "' not found");
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
