package ru.dimsuz.collagecreator.collage;

import android.graphics.RectF;

import java.util.Arrays;
import java.util.List;

/**
 * A set of predefined collage layouts.
 *
 * A collage layout is constructed like this:
 *
 * Collage rectangle is a unit rectangle, its size is 1x1.
 * Each image is represented by a RectF given in that unit coordinate system.
 * For example RectF(0, 0.5, 0.5, 1) will place an image into left part of lower half of a
 * collage.
 *
 * Collage can have an arbitrary amount of items.
 */
public final class CollageLayout {
    public final static List<RectF> SIMPLE_2x2 = Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.49f), new RectF(0.51f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.49f, 0.98f), new RectF(0.51f, 0.51f, 0.98f, 0.98f));
    public final static List<RectF> SIMPLE_2x1 = Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.49f), new RectF(0.51f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.98f, 0.98f));
}
