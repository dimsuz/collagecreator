package ru.dimsuz.collagecreator.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

/**
 * Provides an image view which supports Checkable interface
 */
public class CheckableImageView extends ImageView implements Checkable {
    private boolean isChecked;

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private Drawable foreground;

    public CheckableImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if(isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    public void setForeground(Drawable d) {
        this.foreground = d;
        foreground.setState(getDrawableState());
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(foreground != null) {
            foreground.setBounds(0, 0, getWidth(), getHeight());
            foreground.draw(canvas);
        }
    }

    @Override
    public void setChecked(final boolean checked) {
        if(isChecked == checked)  return;
        isChecked = checked;
        refreshDrawableState();
        foreground.setState(getDrawableState());
    }
}
