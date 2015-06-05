package ru.dimsuz.collagecreator.collage;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.dimsuz.collagecreator.R;

/**
 * A set of predefined collage layouts.
 *
 * A collage layout is constructed like this:
 *
 * Collage image rectangle is a unit rectangle, its size is 1x1.
 * Each image is represented by a RectF given in that unit coordinate system.
 * For example RectF(0, 0.5, 0.5, 1) will place an image into left part of lower half of a
 * collage.
 *
 * Collage can have an arbitrary amount of items.
 */
public final class CollageLayout {
    public final static CollageLayout SIMPLE_2x2 = new CollageLayout(Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.49f), new RectF(0.51f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.49f, 0.98f), new RectF(0.51f, 0.51f, 0.98f, 0.98f)),
            R.string.collage_2x2, 0);
    public final static CollageLayout SIMPLE_2x1 = new CollageLayout(Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.49f), new RectF(0.51f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.98f, 0.98f)),
            R.string.collage_2x1, 0);
    public final static CollageLayout SIMPLE_1x2 = new CollageLayout(Arrays.asList(
            new RectF(0.02f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.49f, 0.98f), new RectF(0.51f, 0.51f, 0.98f, 0.98f)),
            R.string.collage_1x2, 0);
    public final static CollageLayout SIMPLE_1x1_H = new CollageLayout(Arrays.asList(
            new RectF(0.02f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.98f, 0.98f)),
            R.string.collage_1x1_h, 0);
    public final static CollageLayout SIMPLE_1x1_V = new CollageLayout(Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.98f),
            new RectF(0.51f, 0.02f, 0.98f, 0.98f)),
            R.string.collage_1x1_v, 0);
    public final static CollageLayout CRAZY = new CollageLayout(Arrays.asList(
            new RectF(0.01f, 0.01f, 0.595f, 0.595f),
            new RectF(0.605f, 0.01f, 0.99f, 0.395f),
            new RectF(0.605f, 0.405f, 0.99f, 0.695f),
            new RectF(0.605f, 0.705f, 0.99f, 0.99f),
            new RectF(0.305f, 0.605f, 0.595f, 0.99f),
            new RectF(0.01f, 0.605f, 0.295f, 0.795f),
            new RectF(0.01f, 0.805f, 0.295f, 0.99f)),
            R.string.collage_crazy, 0);
    private static int NEXT_ID = 0;

    public final List<RectF> rects;
    public final int descriptionResId;
    public final int iconResId;
    public final long id;

    public CollageLayout(List<RectF> imageRects, int descriptionResId, int iconResId) {
        this.rects = Collections.unmodifiableList(new ArrayList<>(imageRects));
        this.descriptionResId = descriptionResId;
        this.iconResId = iconResId;
        this.id = NEXT_ID++;
    }

    public int size() {
        return rects.size();
    }
}
