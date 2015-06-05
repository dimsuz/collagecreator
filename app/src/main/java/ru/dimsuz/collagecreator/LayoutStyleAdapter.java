package ru.dimsuz.collagecreator;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.dimsuz.collagecreator.collage.CollageLayout;

/**
 * Represents a list of supported collage types
 */
public class LayoutStyleAdapter extends BaseAdapter {
    private final CollageLayout[] layouts;

    public LayoutStyleAdapter() {
        layouts = new CollageLayout[] {
                CollageLayout.SIMPLE_2x2,
                CollageLayout.SIMPLE_2x1,
                CollageLayout.SIMPLE_1x2,
                CollageLayout.SIMPLE_1x1_H,
                CollageLayout.SIMPLE_1x1_V,
                CollageLayout.CRAZY
        };
    }

    // not using view recycling here, because it is very unlikely to occur for the currently
    // not very big layout count, can be introduced later if needed
    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup viewGroup) {
        TextView view = (TextView) LayoutInflater.from(viewGroup.getContext())
                .inflate(android.R.layout.simple_spinner_item, viewGroup, false);
        view.setText(layouts[position].descriptionResId);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup viewGroup) {
        TextView view = (TextView) LayoutInflater.from(viewGroup.getContext())
                .inflate(android.R.layout.simple_spinner_dropdown_item, viewGroup, false);
        view.setText(layouts[position].descriptionResId);
        if(layouts[position].iconResId != 0) {
            view.setCompoundDrawablesWithIntrinsicBounds(layouts[position].iconResId, 0, 0, 0);
        }
        return view;
    }

    @Override
    public int getCount() {
        return layouts.length;
    }

    @Override
    public Object getItem(int i) {
        return layouts[i];
    }

    @Override
    public long getItemId(int i) {
        return layouts[i].id;
    }

    @Nullable
    public CollageLayout getItemById(long id) {
        for(CollageLayout layout : layouts) {
            if(layout.id == id) return layout;
        }
        return null;
    }

    public int getItemPositionById(long id) {
        for(int i = 0, layoutsLength = layouts.length; i < layoutsLength; i++) {
            if(layouts[i].id == id) return i;
        }
        return -1;
    }
}
