package ru.dimsuz.collagecreator.data;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

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
                return compareInts(imageInfo2.likesCount(), imageInfo1.likesCount());
            }
        };
    }

    public static Func2<ImageInfo, ImageInfo, Integer> sortIdsFirst(final List<String> prioritizedIds) {
        return new Func2<ImageInfo, ImageInfo, Integer>() {
            @Override
            public Integer call(ImageInfo imageInfo1, ImageInfo imageInfo2) {
                int priorityIndex1 = prioritizedIds.indexOf(imageInfo1.id());
                int priorityIndex2 = prioritizedIds.indexOf(imageInfo2.id());
                if(priorityIndex1 == -1 && priorityIndex2 == -1) {
                    return compareInts(imageInfo2.likesCount(), imageInfo1.likesCount());
                }
                if(priorityIndex1 == -1) {
                    return 1;
                } else if(priorityIndex2 == -1){
                    return -1;
                } else {
                    return compareInts(priorityIndex1, priorityIndex2);
                }
            }
        };
    }

    public static Comparator<ImageInfo> comparatorByDateDesc() {
        return new Comparator<ImageInfo>() {
            @Override
            public int compare(ImageInfo i1, ImageInfo i2) {
                return compareLongs(i2.timestamp(), i1.timestamp());
            }
        };
    }

    public static Comparator<ImageInfo> comparatorByLikes() {
        return new Comparator<ImageInfo>() {
            @Override
            public int compare(ImageInfo i1, ImageInfo i2) {
                return compareInts(i2.likesCount(), i1.likesCount());
            }
        };
    }

    public static int compareInts(int i1, int i2) {
        // need to be a little weirder than Integer.compare() to support earlier api levels
        // copy paste java 6 implementation
        return (i1 < i2) ? -1 : ((i1 == i2) ? 0 : 1);
    }

    public static int compareLongs(long i1, long i2) {
        return (i1 < i2) ? -1 : ((i1 == i2) ? 0 : 1);
    }
}
