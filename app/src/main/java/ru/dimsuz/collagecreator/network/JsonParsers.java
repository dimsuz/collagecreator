package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Contains a set of function helpers which parse json returned by Instagram servers
 */
public final class JsonParsers {
    @NotNull
    public static UserInfo parseUserInfo(Response response, Gson gson, String userName) throws IOException {
        // FIXME prettify this stuff!
        JsonReader jsonReader = new JsonReader(response.body().charStream());
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("data")) {
                UserResponseData[] result = gson.fromJson(jsonReader, UserResponseData[].class);
                Timber.d("got this!\n%s", Arrays.asList(result));
                UserResponseData match = findBestUserNameMatch(result, userName);
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
        throw new RuntimeException("username '" + userName + "' not found");
    }

    @Nullable
    private static UserResponseData findBestUserNameMatch(UserResponseData[] result, String userName) {
        if(result.length == 0) { return null; }
        // for some reason data is returned in not sorted by best match order,
        // (for example 'fairylitte' query produces 'fairylittlefingers' first and 'fairylittle'
        // goes later!)
        // so need to search for the
        // exact match otherwise use first supplied!
        Timber.d("got %d matches for %s, searching for exact match", result.length, userName);
        for(int i = 0, sz = result.length; i < sz; i++) {
            if(result[i].username.equals(userName)) {
                return result[i];
            }
        }
        return null;
    }

    /**
     * Used while deserializing user info json
     */
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
}
