package com.filestack.android;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.ColorInt;

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

        /**
         * Title of the Picker.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Background color. Displayed in lists and a navigation drawer.
         * Used also as a text color for toolbar title and
         *     buttons on authorization/local/camera screens.
         * @param backgroundColor int representation of a color
         */
        public Builder backgroundColor(@ColorInt int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * Accent color. Used as a color for toolbar, selection markers and navigation drawer items.
         * @param accentColor int representation of a color
         */
        public Builder accentColor(@ColorInt int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        /**
         * Text color. Used as a color for text in camera/local/authorization/file list screens.
         * @param textColor int representation of a color
         */
        public Builder textColor(@ColorInt int textColor) {
            this.textColor = textColor;
            return this;
        }

        /**
         * Builds a new theme based on provided parameters.
         * @return new {@link Theme} instance
         */
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
