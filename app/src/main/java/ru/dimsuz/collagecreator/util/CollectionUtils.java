package ru.dimsuz.collagecreator.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic utility helpers for manipulating collections
 */
public final class CollectionUtils {
    /**
     * Returns a new list with 'count' items copied from start of source list
     */
    @NotNull
    public <T> List<T> limit(List<T> list, int count) {
        if(list == null || list.isEmpty() || count < 0) { return new ArrayList<>(); }
        int sz = Math.min(list.size(), count);
        ArrayList<T> result = new ArrayList<>(sz);
        for(int i=0; i<sz; i++) result.add(list.get(i));
        return result;
    }
}
