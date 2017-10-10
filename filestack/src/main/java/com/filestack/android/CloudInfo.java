package com.filestack.android;

class CloudInfo {
    private String provider;
    private int iconId;
    private int textId;

    CloudInfo(String provider, int iconId, int textId) {
        this.provider = provider;
        this.iconId = iconId;
        this.textId = textId;
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
