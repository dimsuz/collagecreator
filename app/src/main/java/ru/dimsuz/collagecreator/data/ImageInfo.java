package ru.dimsuz.collagecreator.data;

import org.jetbrains.annotations.NotNull;

import auto.parcel.AutoParcel;
import rx.functions.Func2;

@AutoParcel
public abstract class ImageInfo {
    public abstract String id();

    public abstract String url();
    public abstract String title();
    public abstract int likesCount();
    public abstract int width();
    public abstract int height();
    public abstract long timestamp();
    public static ImageInfo create(String id, String url, String title, int likesCount, int width, int height, long timestamp) {
        return new AutoParcel_ImageInfo(id, url, title, likesCount, width, height, timestamp);
    }

    /**
     * @return a function to sort images by likes count DESC
     */
    @NotNull
    public static Func2<ImageInfo, ImageInfo, Integer> sortByLikesDesc() {
        return new Func2<ImageInfo, ImageInfo, Integer>() {
            @Override
            public Integer call(ImageInfo imageInfo1, ImageInfo imageInfo2) {
                // need to be a little weirder than Integer.compare() to support earlier api levels
                return Integer.valueOf(imageInfo2.likesCount()).compareTo(imageInfo1.likesCount());
            }
        };
    }
}
