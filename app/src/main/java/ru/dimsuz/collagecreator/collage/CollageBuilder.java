package ru.dimsuz.collagecreator.collage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ThumbnailUtils;

import java.util.List;

import timber.log.Timber;

/**
 * Builds collages with different configurations
 */
public class CollageBuilder {
    /**
     * Builds a collage out of a list of images
     *
     * @param images a list of images to use
     * @param layout a collage layout to use. See {@link CollageLayout} for description of format
     * @param targetSize a target size (in px) for this collage
     * @param bgColor a background color to use
     * @return a collage bitmap
     */
    public static Bitmap create(List<Bitmap> images, CollageLayout layout, int targetSize, int bgColor) {
        validateArguments(images, layout, targetSize);
        // if sizes do not much, use as much data as possible
        int count = Math.min(images.size(), layout.size());
        Timber.d("building collage out of %d images", count);
        RectF[] rects = mapToTargetRect(layout.rects, targetSize, count);
        Bitmap bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.RGB_565);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);
        for(int i=0; i<rects.length; i++) {
            Bitmap src = images.get(i);
            Bitmap b = isSameAspectRatio(src, rects[i])
                    ? src : ThumbnailUtils.extractThumbnail(src, (int)rects[i].width(), (int)rects[i].height());
            canvas.drawBitmap(b, null, rects[i], paint);
        }
        if(s++ < 3)
            throw new RuntimeException("fail");
        return bitmap;
    }
    private static int s = 0;

    private static boolean isSameAspectRatio(Bitmap b, RectF rect) {
        return Math.abs(b.getWidth() / (float)b.getHeight() - rect.width() / rect.height()) < 0.0001f;
    }

    private static void validateArguments(List<Bitmap> images, CollageLayout layout, int targetSize) {
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
