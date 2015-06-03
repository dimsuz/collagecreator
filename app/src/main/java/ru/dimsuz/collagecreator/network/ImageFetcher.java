package ru.dimsuz.collagecreator.network;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import ru.dimsuz.collagecreator.data.ImageInfo;
import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Retrieves bitmaps by url
 */
public final class ImageFetcher {
    /**
     * Retrieves an image bitmaps for image info from list
     * @return a not null list of bitmaps
     */
    public static Observable<Bitmap> get(final Context context, final List<ImageInfo> imageInfoList) {
        return Observable.from(imageInfoList).flatMap(new Func1<ImageInfo, Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call(ImageInfo imageInfo) {
                return get(context, imageInfo);
            }
        });
    }

    /**
     * Retrieves an image bitmaps for image info
     */
    public static Observable<Bitmap> get(final Context context, final ImageInfo imageInfo) {
        return Observable.just(imageInfo).map(new Func1<ImageInfo, Bitmap>() {
            @Override
            public Bitmap call(ImageInfo image) {
                try {
                    return Picasso.with(context).load(image.url()).get();
                } catch(IOException e) {
                    Timber.e(e, "failed to retrieve image %s", imageInfo);
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
