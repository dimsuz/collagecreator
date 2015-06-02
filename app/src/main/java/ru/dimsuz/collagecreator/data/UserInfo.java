package ru.dimsuz.collagecreator.data;

import android.text.TextUtils;

import org.jetbrains.annotations.Nullable;

import auto.parcel.AutoParcel;

/**
 * Represents information about instagram user
 */
@AutoParcel
public abstract class UserInfo {
    public abstract String userName();
    @Nullable
    public abstract String userId();

    /**
     * A UserInfo instance which represents an invalid info
     */
    public static UserInfo INVALID_INFO = UserInfo.create("", null);

    public static UserInfo create(String userName, @Nullable String userId) {
        return new AutoParcel_UserInfo(userName, userId);
    }


    public boolean isValid() {
        return !TextUtils.isEmpty(userName()) && !TextUtils.isEmpty(userId());
    }
}
