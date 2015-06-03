package ru.dimsuz.collagecreator.collage;

import android.graphics.RectF;

import java.util.Arrays;
import java.util.List;

/**
 * A set of predefined collage layouts
 */
public final class CollageLayout {
    public final static List<RectF> SIMPLE_2x2 = Arrays.asList(
            new RectF(0.02f, 0.02f, 0.49f, 0.49f), new RectF(0.51f, 0.02f, 0.98f, 0.49f),
            new RectF(0.02f, 0.51f, 0.49f, 0.98f), new RectF(0.51f, 0.51f, 0.98f, 0.98f));
}
