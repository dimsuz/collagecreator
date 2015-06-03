package ru.dimsuz.collagecreator.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * RxJava helper utility functions
 */
public final class Functions {
    /**
     * Flattens a list of items into an observable of those items
     */
    @NotNull
    public static <T> Func1<List<T>, Observable<T>> flatten() {
        return new Func1<List<T>, Observable<T>>() {
            @Override
            public Observable<T> call(List<T> list) {
                return Observable.from(list);
            }
        };
    }
}
