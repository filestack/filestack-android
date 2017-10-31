package com.filestack.android;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class SelectedItem {
    private String provider;
    private String path;
    private Uri uri;

    interface Saver {
        boolean toggleItem(String provider, Uri uri);
        boolean toggleItem(String provider, String path);
        boolean isSelected(String provider, Uri uri);
        boolean isSelected(String provider, String path);
        void setItemChangeListener(ItemChangeListener listener);
        ArrayList<SelectedItem> getItems();
        void clear();
        interface ItemChangeListener {
            void onCountChanged(int newSize);
        }
    }

    public static class SimpleSaver implements Saver {
        private ArrayList<SelectedItem> selectedItems = new ArrayList<>();
        private ItemChangeListener listener;

        @Override
        public boolean toggleItem(String provider, Uri uri) {
            return toggleItem(new SelectedItem(provider, uri));
        }

        @Override
        public boolean toggleItem(String provider, String path) {
            return toggleItem(new SelectedItem(provider, path));
        }

        @Override
        public boolean isSelected(String provider, Uri uri) {
            return isSelected(new SelectedItem(provider, uri));
        }

        @Override
        public boolean isSelected(String provider, String path) {
            return isSelected(new SelectedItem(provider, path));
        }

        @Override
        public void setItemChangeListener(ItemChangeListener listener) {
            this.listener = listener;
        }

        @Override
        public ArrayList<SelectedItem> getItems() {
            return selectedItems;
        }

        @Override
        public void clear() {
            selectedItems.clear();
            callListener();
        }

        private boolean toggleItem(SelectedItem item) {
            boolean isSaved;

            if (isSelected(item)) {
                selectedItems.remove(item);
                isSaved = false;
            } else {
                selectedItems.add(item);
                isSaved = true;
            }

            callListener();
            return isSaved;
        }

        private boolean isSelected(SelectedItem item) {
            return selectedItems.contains(item);
        }

        private void callListener() {
            if (listener != null) {
                listener.onCountChanged(selectedItems.size());
            }
            log();
        }

        private void log() {
            Log.d("selectedItem", "count: " + Integer.toString(selectedItems.size()));
            for (SelectedItem item : selectedItems) {
                Log.d("selectedItem", item.getProvider() + ": "
                        + (item.getPath() != null ? item.getPath() : item.getUri().toString()));
            }
        }
    }

    public SelectedItem(String provider, Uri uri) {
        this.provider = provider;
        this.uri = uri;
    }

    public SelectedItem(String provider, String path) {
        this.provider = provider;
        this.path = path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof SelectedItem)) {
            return false;
        }

        SelectedItem item = (SelectedItem) obj;

        if (!this.getProvider().equals(item.getProvider())) {
            return false;
        } else if (!this.getPath().equals(item.getPath())) {
            return false;
        } else if (this.getUri() != item.getUri()) {
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

    public Uri getUri() {
        return uri;
    }
}
