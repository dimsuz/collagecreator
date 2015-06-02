package ru.dimsuz.collagecreator;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.dimsuz.collagecreator.network.InstagramApi;

@Module(injects = MainActivity.class)
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
    InstagramApi provideInstagramApi(OkHttpClient client) {
        return new InstagramApi(client);
    }
}
