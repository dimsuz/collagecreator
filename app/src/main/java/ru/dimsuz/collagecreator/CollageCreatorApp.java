package ru.dimsuz.collagecreator;

import android.app.Application;
import android.content.Context;

import dagger.ObjectGraph;
import timber.log.Timber;

public class CollageCreatorApp extends Application {
    /**
     * A Dagger's object graph
     */
    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        if(BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        objectGraph = ObjectGraph.create(new MainModule(this));
    }

    public void inject(Object o) {
        objectGraph.inject(o);
    }

    public static CollageCreatorApp get(Context context) {
        return (CollageCreatorApp)context.getApplicationContext();
    }
}
