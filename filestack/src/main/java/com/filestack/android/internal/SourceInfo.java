package com.filestack.android.internal;

public class SourceInfo {
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

    public String getId() {
        return id;
    }

    public int getIconId() {
        return iconId;
    }

    public int getTextId() {
        return textId;
    }

    public int getColorId() {
        return colorId;
    }
}
