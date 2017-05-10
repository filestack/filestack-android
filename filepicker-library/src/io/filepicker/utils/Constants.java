package io.filepicker.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.filepicker.R;
import io.filepicker.models.GoogleDriveNode;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 11/7/14.
 */
public class Constants {

    private Constants() {}

    public static final float ALPHA_NORMAL = 1f;
    public static final float ALPHA_FADED = 0.2f;

    public static final String MIMETYPE_IMAGE = "image/*";
    public static final String MIMETYPE_VIDEO = "video/*";
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
            new Provider("Google Photos",        "GooglePhotos",       MIMETYPE_IMAGE,     R.drawable.glyphicons_366_picasa, true, "PICASA"),
            new Provider("Github",        "Github",       MIMETYPE_ALL,       R.drawable.glyphicons_381_github, false, "GITHUB"),
            new Provider("Google Drive",  "GoogleDrive",       MIMETYPE_ALL,       R.drawable.gdrive, false, "GOOGLE_DRIVE"),
            new Provider("Evernote",  "Evernote",       MIMETYPE_ALL,       R.drawable.evernote, true, "EVERNOTE"),
            new Provider("OneDrive",  "OneDrive",       MIMETYPE_ALL,       R.drawable.onedrive, true, "SKYDRIVE")
    ));

    public final static String TYPE_DRIVE = "GOOGLE_DRIVE";
    public final static String TYPE_GMAIL = "GMAIL";
    public final static String TYPE_PICASA = "PICASA";

    public final static ArrayList<String> NATIVE_PROVIDERS = new ArrayList<>(Arrays.asList(TYPE_DRIVE,TYPE_GMAIL,TYPE_PICASA));

    public static final String LIST_VIEW   = "list";
    public static final String THUMBNAILS_VIEW  = "thumbnails";
    public static final String TERMINATING  = "Terminating Operations...";

    public static class ExportObject {
        public String label;
        public String extension;

        public ExportObject(String label, String extension) {
            this.label = label;
            this.extension = extension;
        }

        public String toString(){
            return label;
        }
    }

    public static HashMap<String,ExportObject> exportMap = new HashMap<>();

    static {
        exportMap.put("text/html",new ExportObject("HTML",".html"));
        exportMap.put("text/plain",new ExportObject("Plain text",".txt"));
        exportMap.put("application/rtf",new ExportObject("Rich text",".rtf"));
        exportMap.put("application/vnd.oasis.opendocument.text",new ExportObject("Open Office doc",".odt"));
        exportMap.put("application/pdf",new ExportObject("PDF",".pdf"));
        exportMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",new ExportObject("MS Word document",".docx"));
        exportMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",new ExportObject("MS Excel",".xlsx"));
        //exportMap.put("application/x-vnd.oasis.opendocument.spreadsheet",new ExportObject("Open Office sheet",".ods"));
        exportMap.put("application/vnd.oasis.opendocument.spreadsheet",new ExportObject("Open Office sheet",".ods"));
        exportMap.put("text/csv",new ExportObject("CSV (first sheet only)",".csv"));
        exportMap.put("image/jpeg",new ExportObject("JPEG",".jpg"));
        exportMap.put("image/png",new ExportObject("PNG",".png"));
        exportMap.put("image/svg+xml",new ExportObject("SVG",".svg"));
        exportMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",new ExportObject("MS PowerPoint",".pptx"));
        exportMap.put("application/vnd.google-apps.script+json",new ExportObject("JSON",".json"));
        exportMap.put("application/x-mspublisher",new ExportObject("Publisher",".pub"));
        exportMap.put("text/tab-separated-values",new ExportObject("Tab separated values",".tsv"));
        exportMap.put("application/zip",new ExportObject("Zip file",".zip"));
        exportMap.put("application/epub+zip",new ExportObject("application/epub+zip",".epub"));
    }


    public static String getExtension(GoogleDriveNode gNode){
        return Constants.exportMap.get(gNode.exportFormat) != null ?
                (!gNode.displayName.contains(Constants.exportMap.get(gNode.exportFormat).extension)?Constants.exportMap.get(gNode.exportFormat).extension:""): "";
    }




}
