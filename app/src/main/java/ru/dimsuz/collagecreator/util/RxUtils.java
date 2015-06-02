package ru.dimsuz.collagecreator.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.android.widget.OnTextChangeEvent;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Utils for Reactive Java
 */
public class RxUtils {
    /**
     * Creates a generic observable which executes a given function once, producing a single
     * result and finishing
     *
     * @param func a function to execute. If this function throws a RuntimeException with
     *             some other exception attached to it as a cause, then that attached exception
     *             will be passed to the observers' onError when thrown
     */
    public static <T> Observable<T> runOnce(final Func0<T> func) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(@NotNull Subscriber<? super T> subscriber) {
                if(subscriber.isUnsubscribed()) {
                    return;
                }

                try {
                    T result = func.call();
                    if(!subscriber.isUnsubscribed()) {
                        subscriber.onNext(result);
                    }
                    if(!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                } catch(Throwable e) {
                    if(!subscriber.isUnsubscribed()) {
                        // it is impossible to have a Func0 which throws checked exception,
                        // so client code will need to have try/catch in Func's which will get
                        // passed to this method and possibly rethrow as unchecked exceptions.
                        // So try to check if client code rethrown a RuntimeException which wraps
                        // original exception and report original one to the observers
                        if((e instanceof RuntimeException) && e.getCause() != null) {
                            subscriber.onError(e.getCause());
                        } else {
                            subscriber.onError(e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Executes http request
     */
    public static Observable<Response> httpGetRequest(final OkHttpClient client, final String url) {
        return RxUtils.runOnce(new Func0<Response>() {
            @Override
            public Response call() {
                Timber.d("launching get request: {}", url);
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if(!response.isSuccessful()) {
                        throw new IOException("server returned an error code " + response + ", url: " + url);
                    }
                    Timber.d("request was successful: {}", url);
                    return response;
                } catch(IOException e) {
                    Timber.e("request failed: {}", url);
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
