package com.filestack.android;

public class FsConstants {
    // Options for the picker UI
    /** Expects boolean value. Selections won't get uploaded automatically if set to false. */
    public static final String EXTRA_AUTO_UPLOAD = "autoUpload";
    /** Expects {{@link com.filestack.Config}}. Sets credential and other account info. */
    public static final String EXTRA_CONFIG = "config";
    /** Expects {{@link java.util.ArrayList<String>}}. Sets sources to enable. */
    public static final String EXTRA_SOURCES = "sources";
    /** Expects {{@link com.filestack.StorageOptions}}. Sets options for how to store uploads. */
    public static final String EXTRA_STORE_OPTS = "storeOpts";
    /** Expects string array. Sets MIME types that are allowed to be selected. */
    public static final String EXTRA_MIME_TYPES = "mimeTypes";
    /**
     * Expects boolean. If true, multiple files can be selected for upload.
     * Settings this value to false restricts the selection to only one file.
     * Defaults to true.
     */
    public static final String EXTRA_ALLOW_MULTIPLE_FILES = "multipleFiles";

    /** Action for upload broadcast intent filter. */
    public static final String BROADCAST_UPLOAD = "com.filestack.android.BROADCAST_UPLOAD";

    // Keys to access data from result intents
    /** For selections, returns {{@link java.util.ArrayList}} of {{@link Selection}} objects. */
    public static final String EXTRA_SELECTION_LIST = "selectionList";
    /** For uploads, returns the {{@link Selection}} that corresponded to this upload. */
    public static final String EXTRA_SELECTION = "selection";
    /** For uploads, returns upload metadata as {{@link com.filestack.FileLink}}. */
    public static final String EXTRA_FILE_LINK = "fileLink";
    /** For uploads, returns upload status string. */
    public static final String EXTRA_STATUS = "status";
    /** Indicates an upload completed successfully. */
    public static final String STATUS_COMPLETE = "complete";
    /** Indicates an upload failed. */
    public static final String STATUS_FAILED = "failed";

}
