package ru.dimsuz.collagecreator.data;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class ImageInfo {
    public abstract String url();
    public abstract String title();
    public abstract int likesCount();
    public abstract int width();
    public abstract int height();
    public abstract long timestamp();

    public static ImageInfo create(String url, String title, int likesCount, int width, int height, long timestamp) {
        return new AutoParcel_ImageInfo(url, title, likesCount, width, height, timestamp);
    }
}
