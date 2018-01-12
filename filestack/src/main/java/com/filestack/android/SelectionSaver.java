package com.filestack.android;

import java.util.ArrayList;

public interface SelectionSaver {
    boolean toggleItem(String provider, String path, String name);
    boolean isSelected(String provider, String path, String name);
    void setItemChangeListener(Listener listener);
    ArrayList<Selection> getItems();
    void clear();
    boolean isEmpty();
    interface Listener {
        void onEmptyChanged(boolean isEmpty);
    }
}
