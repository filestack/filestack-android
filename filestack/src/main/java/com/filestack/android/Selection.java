package com.filestack.android;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class Selection implements Serializable {
    private String provider;
    private String path;
    private String name;

    interface Saver {
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

    public static class SimpleSaver implements Saver {
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
            this.listener = listener;
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

    public Selection(String provider, String path, String name) {
        this.provider = provider;
        this.path = path;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Selection)) {
            return false;
        }

        Selection item = (Selection) obj;

        if (!this.getProvider().equals(item.getProvider())) {
            return false;
        } else if (!this.getPath().equals(item.getPath())) {
            return false;
        } else if (!this.getName().equals(item.getName())) {
            return false;
        }

        return true;
    }

    public String getProvider() {
        return provider;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
