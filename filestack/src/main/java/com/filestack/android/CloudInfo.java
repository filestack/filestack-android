package com.filestack.android;

class CloudInfo {
    private int id;
    private String provider;
    private int iconId;
    private int textId;

    CloudInfo(int id, String provider, int iconId, int textId) {
        this.id = id;
        this.provider = provider;
        this.iconId = iconId;
        this.textId = textId;
    }

    public int getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public int getIconId() {
        return iconId;
    }

    public int getTextId() {
        return textId;
    }
}
