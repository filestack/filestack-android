package com.filestack.android.internal;

import com.filestack.android.Selection;

import java.util.ArrayList;

public interface SelectionSaver {
    boolean toggleItem(Selection selection);
    boolean isSelected(Selection selection);
    void setItemChangeListener(Listener listener);
    ArrayList<Selection> getItems();
    void clear();
    boolean isEmpty();
    interface Listener {
        void onEmptyChanged(boolean isEmpty);
    }
}
