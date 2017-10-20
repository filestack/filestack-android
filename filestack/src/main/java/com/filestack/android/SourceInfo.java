package com.filestack.android;

class SourceInfo {
    private String id;
    private int iconId;
    private int textId;
    private int colorId;

    SourceInfo(String id, int iconId, int textId, int colorId) {
        this.id = id;
        this.iconId = iconId;
        this.textId = textId;
        this.colorId = colorId;
    }

    String getId() {
        return id;
    }

    int getIconId() {
        return iconId;
    }

    int getTextId() {
        return textId;
    }

    int getColorId() {
        return colorId;
    }
}
