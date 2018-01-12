package com.filestack.android.internal;

import android.util.Log;

import com.filestack.android.Selection;

import java.util.ArrayList;

public class SimpleSelectionSaver implements SelectionSaver {
    private ArrayList<Selection> selections = new ArrayList<>();
    private Listener listener;

    @Override
    public boolean toggleItem(String provider, String path, String name) {
        return toggleItem(new Selection(provider, path, name));
    }

    @Override
    public boolean isSelected(String provider, String path, String name) {
        return isSelected(new Selection(provider, path, name));
    }

    @Override
    public void setItemChangeListener(Listener listener) {
        if (listener == null) {
            return;
        }
        this.listener = listener;
        this.listener.onEmptyChanged(isEmpty());
    }

    @Override
    public ArrayList<Selection> getItems() {
        return selections;
    }

    @Override
    public void clear() {
        if (selections.size() != 0) {
            selections.clear();
            if (listener != null) {
                listener.onEmptyChanged(true);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return selections.size() == 0;
    }

    private boolean toggleItem(Selection item) {
        boolean isSaved;

        boolean wasEmpty = isEmpty();

        if (isSelected(item)) {
            selections.remove(item);
            isSaved = false;
        } else {
            selections.add(item);
            isSaved = true;
        }

        boolean isEmpty = isEmpty();

        if (listener != null && wasEmpty != isEmpty) {
            listener.onEmptyChanged(isEmpty);
        }

        return isSaved;
    }

    private boolean isSelected(Selection item) {
        return selections.contains(item);
    }

    private void log() {
        Log.d("selectedItem", "count: " + Integer.toString(selections.size()));
        for (Selection item : selections) {
            Log.d("selectedItem", item.getProvider() + ": " + item.getPath());
        }
    }
}
