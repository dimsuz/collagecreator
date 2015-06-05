package ru.dimsuz.collagecreator.collage;

import android.content.Context;
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

    private final List<RectF> rects;
    private final int descriptionResId;
    private final int iconResId;

    public CollageLayout(List<RectF> imageRects, int descriptionResId, int iconResId) {
        this.rects = Collections.unmodifiableList(new ArrayList<>(imageRects));
        this.descriptionResId = descriptionResId;
        this.iconResId = iconResId;
    }

    public List<RectF> rects() {
        return rects;
    }

    public int size() {
        return rects.size();
    }

    public String getDescription(Context context) {
        return context.getString(descriptionResId);
    }

    public int getDescriptionRes() {
        return descriptionResId;
    }

    public int getIconRes() {
        return iconResId;
    }
}
