package ru.dimsuz.collagecreator.collage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.List;

import timber.log.Timber;

/**
 * Builds collages with different configurations
 */
public class CollageBuilder {
    // FIXME describe!
    public static Bitmap create(List<Bitmap> images, List<RectF> layout, int targetSize, int bgColor) {
        validateArguments(images, layout, targetSize);
        // if sizes do not much, use as much data as possible
        int count = Math.min(images.size(), layout.size());
        Timber.d("building collage out of %d images", count);
        RectF[] rects = mapToTargetRect(layout, targetSize, count);
        Bitmap bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.RGB_565);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);
        for(int i=0; i<rects.length; i++) {
            canvas.drawBitmap(images.get(i), null, rects[i], paint);
        }
        return bitmap;
    }

    private static void validateArguments(List<Bitmap> images, List<RectF> layout, int targetSize) {
        if(images == null || layout == null) {
            throw new IllegalArgumentException("not null arguments expected");
        }
        if(targetSize <= 0) {
            throw new IllegalArgumentException("targetWidth is too small! targetSize= " + targetSize);
        }
        if(images.size() != layout.size()) {
            Timber.d("warning! image count and layout placeholder count do not match, will use as much as possible");
        }
    }

    private static RectF[] mapToTargetRect(List<RectF> layout, int targetWidth, int count) {
        // layout contains rects with coordinates specified in a unit coordinate system:
        // 1x1 - is whole collage, and for example rect {(0.5,0.5)-(1.0,1.0)} is rectangle in
        // bottom-right quarter of the collage
        RectF[] result = new RectF[count];
        for(int i=0; i<count; i++) {
            result[i] = new RectF(
                    layout.get(i).left * targetWidth,
                    layout.get(i).top * targetWidth,
                    layout.get(i).right * targetWidth,
                    layout.get(i).bottom * targetWidth);

        }
        return result;
    }

    private static int totalHeight(RectF[] rects) {
        float minTop = Integer.MAX_VALUE;
        float maxBottom = Integer.MIN_VALUE;
        for(int i=0; i<rects.length; i++) {
            minTop = Math.min(rects[i].top, minTop);
            maxBottom = Math.max(rects[i].bottom, maxBottom);
        }
        return Math.round(maxBottom - minTop);
    }
}