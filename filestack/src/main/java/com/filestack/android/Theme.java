package com.filestack.android;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;

public class Theme implements Parcelable {

    static Theme defaultTheme() {
        return new Builder().build();
    }

    private final String title;
    private final int backgroundColor;
    private final int accentColor;
    private final int textColor;

    private Theme(Builder builder) {
        this.title = builder.title;
        this.backgroundColor = builder.backgroundColor;
        this.accentColor = builder.accentColor;
        this.textColor = builder.textColor;
    }

    public String getTitle() {
        return title;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public static class Builder {
        String title = "Filestack Picker";
        int backgroundColor = Color.WHITE;
        int accentColor = Color.parseColor("#FF9800");
        int textColor = Color.parseColor("#89000000");

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder backgroundColor(@ColorInt int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder accentColor(@ColorInt int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder textColor(@ColorInt int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Theme build() {
            return new Theme(this);
        }
    }

    protected Theme(Parcel in) {
        title = in.readString();
        backgroundColor = in.readInt();
        accentColor = in.readInt();
        textColor = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(backgroundColor);
        dest.writeInt(accentColor);
        dest.writeInt(textColor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Theme> CREATOR = new Creator<Theme>() {
        @Override
        public Theme createFromParcel(Parcel in) {
            return new Theme(in);
        }

        @Override
        public Theme[] newArray(int size) {
            return new Theme[size];
        }
    };
}
