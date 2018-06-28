package com.filestack.android.internal;

/** Holds res info about a "source" files can be selected from e.g. local, google, facebook. */
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
