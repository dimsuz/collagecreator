package ru.dimsuz.collagecreator;

import android.support.v4.util.LruCache;

import com.squareup.okhttp.OkHttpClient;

import java.util.List;
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
}
