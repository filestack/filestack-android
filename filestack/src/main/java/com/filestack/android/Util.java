package com.filestack.android;

import android.support.v4.util.ArrayMap;
import android.widget.TextView;

import java.util.Map;

public class Util {
    private static final Map<Integer, CloudInfo> CLOUDS = new ArrayMap<>();

    static {
        CLOUDS.put(R.id.nav_camera, new CloudInfo(
                R.id.nav_camera, null, 0xFFF44336, R.string.menu_camera));
        CLOUDS.put(R.id.nav_file_browser, new CloudInfo(
                R.id.nav_file_browser, null, 0xFFE91E63, R.string.menu_file_browser));
        CLOUDS.put(R.id.nav_facebook, new CloudInfo(
                R.id.nav_facebook, FilestackAndroidClient.CLOUD_FACEBOOK, 0xFF9C27B0, R.string.menu_facebook));
        CLOUDS.put(R.id.nav_flickr, new CloudInfo(
                R.id.nav_flickr, FilestackAndroidClient.CLOUD_FLICKR, 0xFF673AB7, R.string.menu_flickr));
        CLOUDS.put(R.id.nav_google_photos, new CloudInfo(
                R.id.nav_google_photos, FilestackAndroidClient.CLOUD_GOOGLE_PHOTOS, 0xFF3F51B5, R.string.menu_google_photos));
        CLOUDS.put(R.id.nav_instagram, new CloudInfo(
                R.id.nav_instagram, FilestackAndroidClient.CLOUD_INSTAGRAM, 0xFF2196F3, R.string.menu_instagram));
        CLOUDS.put(R.id.nav_amazon_drive, new CloudInfo(
                R.id.nav_amazon_drive, FilestackAndroidClient.CLOUD_AMAZON_DRIVE, 0xFF03A9F4, R.string.menu_amazon_drive));
        CLOUDS.put(R.id.nav_box, new CloudInfo(
                R.id.nav_box, FilestackAndroidClient.CLOUD_BOX, 0xFF00BCD4, R.string.menu_box));
        CLOUDS.put(R.id.nav_dropbox, new CloudInfo(
                R.id.nav_dropbox, FilestackAndroidClient.CLOUD_DROPBOX, 0xFF009688, R.string.menu_dropbox));
        CLOUDS.put(R.id.nav_google_drive, new CloudInfo(
                R.id.nav_google_drive, FilestackAndroidClient.CLOUD_GOOGLE_DRIVE, 0xFF4CAF50, R.string.menu_google_drive));
        CLOUDS.put(R.id.nav_one_drive, new CloudInfo(
                R.id.nav_one_drive, FilestackAndroidClient.CLOUD_ONEDRIVE, 0xFF8BC34A, R.string.menu_one_drive));
        CLOUDS.put(R.id.nav_evernote, new CloudInfo(
                R.id.nav_evernote, FilestackAndroidClient.CLOUD_EVERNOTE, 0xFFCDDC39, R.string.menu_evernote));
        CLOUDS.put(R.id.nav_github, new CloudInfo(
                R.id.nav_github, FilestackAndroidClient.CLOUD_GITHUB, 0xFFFFEB3B, R.string.menu_github));
        CLOUDS.put(R.id.nav_gmail, new CloudInfo(
                R.id.nav_gmail, FilestackAndroidClient.CLOUD_GMAIL, 0xFFFFC107, R.string.menu_gmail));
    }
    
    static CloudInfo getCloudInfo(int id) {
        return CLOUDS.get(id);
    }

    static void textViewReplace(TextView view, String target, String replacement) {
        String text = view.getText().toString();
        text = text.replace(target, replacement);
        view.setText(text);
    }
}
