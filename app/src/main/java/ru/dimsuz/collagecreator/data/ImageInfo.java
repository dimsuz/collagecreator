package ru.dimsuz.collagecreator.data;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class ImageInfo {
    public abstract String url();
    public abstract int likesCount();
}
