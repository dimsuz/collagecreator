package ru.dimsuz.collagecreator;

import android.graphics.Typeface;
import android.support.v4.util.LruCache;

import com.squareup.okhttp.OkHttpClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.dimsuz.collagecreator.data.ImageInfo;
import ru.dimsuz.collagecreator.network.InstagramClient;

@Module(injects = {
        MainActivity.class,
        CollageActivity.class,
        PhotoChooserActivity.class
})
public class MainModule {
    private final CollageCreatorApp application;

    public MainModule(CollageCreatorApp application) {
        this.application = application;
    }

    @Provides @Singleton
    OkHttpClient provideHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);
        client.setConnectTimeout(30, TimeUnit.SECONDS);
        return client;

    }

    @Provides @Singleton
    InstagramClient provideInstagramClient(OkHttpClient client) {
        return new InstagramClient(client);
    }

    @Provides @Singleton
    LruCache<String, List<ImageInfo>> provideImageInfoCache() {
        return new LruCache<>(5);
    }

    @Provides @Singleton
    Map<String, Typeface> provideTypefaceCache() {
        // Typeface cache is needed partly because of the memory leak which was not fixed prior to Honeycomb...
        // otherwise... it's a potential speed up
        Map<String, Typeface> cache = new HashMap<>(3);
        cache.put("Roboto Medium", Typeface.createFromAsset(application.getAssets(), "fonts/Roboto-Medium.ttf"));
        cache.put("Roboto Light", Typeface.createFromAsset(application.getAssets(), "fonts/Roboto-Light.ttf"));
        cache.put("Roboto Regular", Typeface.createFromAsset(application.getAssets(), "fonts/Roboto-Regular.ttf"));
        return Collections.unmodifiableMap(cache);
    }
}
