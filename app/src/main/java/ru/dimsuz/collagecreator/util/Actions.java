package ru.dimsuz.collagecreator.util;

import org.jetbrains.annotations.NotNull;

import ru.dimsuz.collagecreator.data.UserInfo;
import rx.android.widget.OnTextChangeEvent;
import rx.functions.Func1;

/**
 * Various utility actions used across the app
 */
public final class Actions {

    /**
     * A function which will return true if passed userInfo is valid
     */
    @NotNull
    public static Func1<UserInfo, Boolean> validUserInfo() {
        return new Func1<UserInfo, Boolean>() {
            @Override
            public Boolean call(UserInfo userInfo) {
                return userInfo.isValid();
            }
        };
    }

    @NotNull
    public static Func1<OnTextChangeEvent, Boolean> notEmptyText() {
        return new Func1<OnTextChangeEvent, Boolean>() {
            @Override
            public Boolean call(OnTextChangeEvent event) {
                return event.text().length() != 0;
            }
        };
    }
}
