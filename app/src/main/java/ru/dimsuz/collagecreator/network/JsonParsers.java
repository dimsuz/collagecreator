package ru.dimsuz.collagecreator.network;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.data.UserInfo;
import timber.log.Timber;

/**
 * Contains a set of function helpers which parse json returned by Instagram servers
 */
public final class JsonParsers {
    @NotNull
    public static UserInfo parseUserInfo(Response response, Gson gson, String userName) throws IOException {
        JsonReader jsonReader = new JsonReader(response.body().charStream());
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("data")) {
                UserResponseData[] result = gson.fromJson(jsonReader, UserResponseData[].class);
                //Timber.d("got this!\n%s", Arrays.asList(result));
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

    @NotNull
    public static List<ImageInfo> parseImagesResponse(Response response) throws IOException {
        JsonReader jsonReader = new JsonReader(response.body().charStream());
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("data")) {
                return parseImageDataArray(jsonReader);
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        throw new RuntimeException("failed to find user image json data, wrong format?");
    }

    private static List<ImageInfo> parseImageDataArray(JsonReader jsonReader) throws IOException {
        // instagram default to pagination of 20 items per request
        List<ImageInfo> imageList = new ArrayList<>(20);
        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            jsonReader.beginObject();
            int likesCount = 0;
            ImageData imageData = null;
            CaptionData captionData = null;
            while(jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                if(key.equals("likes")) {
                    likesCount = parseLikesCount(jsonReader);
                } else if(key.equals("images")) {
                    imageData = parseImageEntry(jsonReader);
                } else if(key.equals("caption")) {
                    captionData = parseCaption(jsonReader);
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if(imageData != null) {
                imageList.add(ImageInfo.create(imageData.url, captionData != null ? captionData.title : "",
                        likesCount, imageData.width, imageData.height,
                        captionData != null ? captionData.timestamp : 0));
            } else {
                throw new RuntimeException("failed to read required image fields");
            }
            // FIXME use object pools for imageData,captionData
        }
        jsonReader.endArray();
        return imageList;
    }

    private static int parseLikesCount(JsonReader jsonReader) throws IOException {
        int likesCount = 0;
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("count")) {
                likesCount = Integer.parseInt(jsonReader.nextString());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return likesCount;
    }

    @NotNull
    private static ImageData parseImageEntry(JsonReader jsonReader) throws IOException {
        ImageData imageData = null;
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("standard_resolution")) {
                String url = null;
                int width = 0, height = 0;
                jsonReader.beginObject();
                while(jsonReader.hasNext()) {
                    String ikey = jsonReader.nextName();
                    if(ikey.equals("url")) {
                        url = jsonReader.nextString();
                    } else if(ikey.equals("width")) {
                        width = Integer.parseInt(jsonReader.nextString());
                    } else if(ikey.equals("height")) {
                        height = Integer.parseInt(jsonReader.nextString());
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                if(url != null && width != 0 && height != 0) {
                    imageData = new ImageData(url, width, height);
                }
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        if(imageData == null) {
            throw new RuntimeException("no image data in standard resolution, nonsense!");
        }
        return imageData;
    }

    @Nullable
    private static CaptionData parseCaption(JsonReader jsonReader) throws IOException {
        long timestamp = 0;
        String title = null;
        if(jsonReader.peek() == JsonToken.NULL) {
            // It turns out caption data can be missing and be just of 'null' value
            jsonReader.nextNull();
            return null;
        }
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("created_time")) {
                timestamp = Long.parseLong(jsonReader.nextString()) * 1000; // make it in ms
            } else if(key.equals("text")) {
                title = jsonReader.nextString();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        if(title == null) {
            title = "";
        }
        return new CaptionData(title, timestamp);
    }

    private final static class ImageData {
        public String url;
        public int width;
        public int height;

        private ImageData(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    private final static class CaptionData {
        public final String title;
        public final long timestamp;

        private CaptionData(String title, long timestamp) {
            this.title = title;
            this.timestamp = timestamp;
        }
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
