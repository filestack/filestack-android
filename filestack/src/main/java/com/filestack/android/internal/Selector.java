package com.filestack.android.internal;

import com.filestack.android.Selection;

public interface Selector {
    boolean toggle(Selection selection);
    boolean isSelected(Selection selection);

    class Single implements Selector {

        private final SelectionSaver selectionSaver;

        public Single(SelectionSaver selectionSaver) {
            this.selectionSaver = selectionSaver;
        }

        @Override
        public boolean toggle(Selection selection) {
            if (selectionSaver.isEmpty()) {
                return selectionSaver.toggleItem(selection);
            } else if (selectionSaver.getItems().get(0).equals(selection)) {
                return selectionSaver.toggleItem(selection);
            }
            return false;
        }

        @Override
        public boolean isSelected(Selection selection) {
            return selectionSaver.isSelected(selection);
        }
    }

    class Multi implements Selector {

        private SelectionSaver selectionSaver;

        public Multi(SelectionSaver selectionSaver) {
            this.selectionSaver = selectionSaver;
        }

        @Override
        public boolean toggle(Selection selection) {
            return selectionSaver.toggleItem(selection);
        }

        @Override
        public boolean isSelected(Selection selection) {
            return selectionSaver.isSelected(selection);
        }
    }
}
