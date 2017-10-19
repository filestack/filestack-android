package com.filestack.android;

import android.support.v4.util.ArrayMap;
import android.widget.TextView;

import com.filestack.FsSources;

import java.util.Map;

class Util {
    private static final Map<Integer, SourceInfo> SOURCES = new ArrayMap<>();

    static {
        SOURCES.put(R.id.nav_camera, new SourceInfo(
                FsSources.CAMERA,
                R.drawable.ic_source_camera,
                R.string.source_camera,
                R.color.theme_source_camera));

        SOURCES.put(R.id.nav_device, new SourceInfo(
                FsSources.DEVICE,
                R.drawable.ic_source_folder,
                R.string.source_device,
                R.color.theme_source_device));

        SOURCES.put(R.id.nav_facebook, new SourceInfo(
                FsSources.FACEBOOK,
                R.drawable.ic_source_facebook,
                R.string.source_facebook,
                R.color.theme_source_facebook));

        SOURCES.put(R.id.google_photos, new SourceInfo(
                FsSources.GOOGLE_PHOTOS,
                R.drawable.ic_source_google_photos,
                R.string.source_google_photos,
                R.color.theme_source_google_photos));

        SOURCES.put(R.id.instagram, new SourceInfo(
                FsSources.INSTAGRAM,
                R.drawable.ic_source_instagram,
                R.string.source_instagram,
                R.color.theme_source_instagram));

        SOURCES.put(R.id.amazon_drive, new SourceInfo(
                FsSources.AMAZON_DRIVE,
                R.drawable.ic_source_amazon_drive,
                R.string.source_amazon_drive,
                R.color.theme_source_amazon_drive));

        SOURCES.put(R.id.box, new SourceInfo(
                FsSources.BOX,
                R.drawable.ic_source_box,
                R.string.source_box,
                R.color.theme_source_box));

        SOURCES.put(R.id.dropbox, new SourceInfo(
                FsSources.DROPBOX,
                R.drawable.ic_source_dropbox,
                R.string.source_dropbox,
                R.color.theme_source_dropbox));

        SOURCES.put(R.id.google_drive, new SourceInfo(
                FsSources.GOOGLE_DRIVE,
                R.drawable.ic_source_google_drive,
                R.string.source_google_drive,
                R.color.theme_source_google_drive));

        SOURCES.put(R.id.onedrive, new SourceInfo(
                FsSources.ONEDRIVE,
                R.drawable.ic_source_onedrive,
                R.string.source_onedrive,
                R.color.theme_source_onedrive));

        SOURCES.put(R.id.github, new SourceInfo(
                FsSources.GITHUB,
                R.drawable.ic_source_github,
                R.string.source_github,
                R.color.theme_source_github));

        SOURCES.put(R.id.gmail, new SourceInfo(
                FsSources.GMAIL,
                R.drawable.ic_source_gmail,
                R.string.source_gmail,
                R.color.theme_source_gmail));
    }
    
    static SourceInfo getSourceInfo(int id) {
        return SOURCES.get(id);
    }

    static void textViewReplace(TextView view, String target, String replacement) {
        String text = view.getText().toString();
        text = text.replace(target, replacement);
        view.setText(text);
    }
}
