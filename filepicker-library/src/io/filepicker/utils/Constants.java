package io.filepicker.utils;

import java.util.ArrayList;
import java.util.Arrays;

import io.filepicker.R;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 11/7/14.
 */
public class Constants {

    private Constants(){}

    public static final float ALPHA_NORMAL = 1f;
    public static final float ALPHA_FADED = 0.2f;

    public static final String MIMETYPE_IMAGE = "image/*";
    public static final String MIMETYPE_ALL = "*/*";

    public static final String EXTENSION_JPEG = "jpeg";
    public static final String EXTENSION_JPG = "jpg";
    public static final String EXTENSION_PNG = "png";

    public static final String CACHED_FILES_PREFIX = "io_filepicker_library_";

    public final static ArrayList<Provider> PROVIDERS_LIST = new ArrayList<>(Arrays.asList(
            new Provider("Gallery",       "Gallery",      MIMETYPE_IMAGE ,    R.drawable.glyphicons_008_film, false, "GALLERY"),
            new Provider("Camera",        "Camera",       MIMETYPE_IMAGE,     R.drawable.glyphicons_011_camera, false, "CAMERA"),
            new Provider("Facebook",      "Facebook",     MIMETYPE_IMAGE,     R.drawable.glyphicons_390_facebook, false, "FACEBOOK"),
            new Provider("Amazon Cloud Drive", "Clouddrive", MIMETYPE_ALL, R.drawable.ic_amazon_cloud_drive, true, "CLOUDDRIVE"),
            new Provider("Dropbox",       "Dropbox",      MIMETYPE_ALL,       R.drawable.glyphicons_361_dropbox, true, "DROPBOX"),
            new Provider("Box",           "Box",          MIMETYPE_ALL,       R.drawable.glyphicons_154_show_big_thumbnails, true, "BOX"),
            new Provider("Gmail",         "Gmail",        MIMETYPE_ALL,       R.drawable.glyphicons_399_email, false, "GMAIL"),
            new Provider("Instagram",     "Instagram",    MIMETYPE_IMAGE,     R.drawable.instagram, true, "INSTAGRAM"),
            new Provider("Flickr",        "Flickr",       MIMETYPE_IMAGE,     R.drawable.glyphicons_395_flickr, true, "FLICKR"),
            new Provider("Picasa",        "Picasa",       MIMETYPE_IMAGE,     R.drawable.glyphicons_366_picasa, true, "PICASA"),
            new Provider("Github",        "Github",       MIMETYPE_ALL,       R.drawable.glyphicons_381_github, false, "GITHUB"),
            new Provider("Google Drive",  "GoogleDrive",       MIMETYPE_ALL,       R.drawable.gdrive, false, "GOOGLE_DRIVE")
    ));

    public static final String LIST_VIEW   = "list";
    public static final String THUMBNAILS_VIEW  = "thumbnails";
}
