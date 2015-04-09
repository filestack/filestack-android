package io.filepicker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public final class PreferencesUtils {

    private static final String KEY_PREFERENCES = "io.filepicker.library";

    private static final String KEY_SESSION_COOKIE = "sessionCookie";
    private static final String KEY_MULTIPLE = "multiple";
    private static final String KEY_MIMETYPES = "mimetypes";
    private static final String KEY_LOCATION = "storeLocation";
    private static final String KEY_PATH = "storePath";
    private static final String KEY_CONTAINER = "storeContainer";
    private static final String KEY_ACCESS = "storeAccess";
    private static final String KEY_MAX_FILES = "maxFiles";
    private static final String KEY_MAX_SIZE = "maxSize";
    private static final String KEY_SHOW_ERROR_TOAST = "showErrorToast";

    private static final String KEY_SECRET = "app_secret";
    private static final String KEY_POLICY_CALLS = "policy_calls";
    private static final String KEY_POLICY_HANDLE = "policy_handle";
    private static final String KEY_POLICY_EXPIRY = "policy_expiry";
    private static final String KEY_POLICY_MAX_SIZE = "policy_max_size";
    private static final String KEY_POLICY_MIN_SIZE = "policy_min_size";
    private static final String KEY_POLICY_PATH = "policy_path";
    private static final String KEY_POLICY_CONTAINER = "policy_container";

    private final Context context;

    private static PreferencesUtils prefUtils = null;
    private Integer maxFiles;

    private PreferencesUtils(Context context) {
        this.context = context;
    }

    public static PreferencesUtils newInstance(Context context) {
        if(prefUtils == null) {
            prefUtils = new PreferencesUtils(context);
        }

        return prefUtils;
    }

    private SharedPreferences getSharedPreferences(){
        return context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE);
    }

    // String
    private void setStringValue(String key, String value){
        getSharedPreferences().edit().putString(key, value).apply();
    }

    private String getStringValue(String key){
        return getSharedPreferences().getString(key, null);
    }

    // Boolean
    private void setBooleanValue(String key, Boolean value){
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }

    private Boolean getBooleanValue(String key){
        return getSharedPreferences().getBoolean(key, false);
    }

    // Int
    private void setIntValue(String key, int value){
        getSharedPreferences().edit().putInt(key, value).apply();
    }

    private int getIntValue(String key){
        return getSharedPreferences().getInt(key, 0);
    }

    public void setSessionCookie(String sessionCookie) {
        setStringValue(KEY_SESSION_COOKIE, sessionCookie);

        // Whenever session cookie is changed we need to update our ImageLoader
        ImageLoader.setImageLoader(context);
    }

    public String getSessionCookie() {
        return getStringValue(KEY_SESSION_COOKIE);
    }

    public void clearSessionCookie() {
        getSharedPreferences().edit().remove(KEY_SESSION_COOKIE).commit();
    }

    public void setMultiple(boolean allowMultiple) {
        setBooleanValue(KEY_MULTIPLE, allowMultiple);
    }

    public boolean getMultiple() {
        return getBooleanValue(KEY_MULTIPLE);
    }

    public void clearMultiple() {
        setBooleanValue(KEY_MULTIPLE, false);
    }

    // Gets array of mimetypes and saves it as String
    public void setMimetypes(String[] mimetypes) {
        if(mimetypes == null) return;

        StringBuilder mimetypesString = new StringBuilder();
        for(String mimetype : mimetypes) {
            mimetypesString.append(mimetype).append(",");
        }

        setStringValue(KEY_MIMETYPES, mimetypesString.toString());
    }

    // Returns array of mimetypes Strings
    public String[] getMimetypes() {
        String[] mimetypes = null;

        if(getStringValue(KEY_MIMETYPES) != null) {
            mimetypes = getStringValue(KEY_MIMETYPES).split(",");
        }

        return mimetypes;
    }

    public void setPolicyCalls(String[] policyCalls) {
        if(policyCalls == null) return;

        StringBuilder policyCallsString = new StringBuilder();
        for(String policyCall : policyCalls) {
            policyCallsString.append(policyCall).append(",");
        }

        setStringValue(KEY_POLICY_CALLS, policyCallsString.toString());
    }

    // Returns array of mimetypes Strings
    public String[] getPolicyCalls() {
        String[] policyCalls = null;

        if(getStringValue(KEY_POLICY_CALLS) != null) {
            policyCalls = getStringValue(KEY_POLICY_CALLS).split(",");
        }

        return policyCalls;
    }

    public boolean isMimetypeSet(String baseType) {
        String[] mimetypes = getMimetypes();

        if(mimetypes != null) {
            for (String mimetype : mimetypes) {
                if (mimetype.contains(baseType)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void clearMimetypes() {
        setStringValue(KEY_MIMETYPES, null);
    }

    public void setLocation(String location) {
        setStringValue(KEY_LOCATION, location);
    }

    public String getLocation() {
        return getStringValue(KEY_LOCATION);
    }

    public void setPath(String path) {
        setStringValue(KEY_PATH, path);
    }

    public String getPath() {
        return getStringValue(KEY_PATH);
    }

    public void setContainer(String container) {
        setStringValue(KEY_CONTAINER, container);
    }

    public String getContainer() {
        return getStringValue(KEY_CONTAINER);
    }

    public void setMaxFiles(Integer maxFiles) {
        setIntValue(KEY_MAX_FILES, maxFiles);
    }

    public Integer getMaxFiles() {
        return getIntValue(KEY_MAX_FILES);
    }

    public void clearMaxFiles() {
        setIntValue(KEY_MAX_FILES, -1);
    }

    public void setShowErrorToast(Boolean showErrorToast) {
        setBooleanValue(KEY_SHOW_ERROR_TOAST, showErrorToast);
    }

    public Boolean shouldShowErrorToast() {
        return getBooleanValue(KEY_SHOW_ERROR_TOAST);
    }

    public void setMaxSize(Integer maxSize) {
        setIntValue(KEY_MAX_SIZE, maxSize);
    }

    public Integer getMaxSize() {
        return getIntValue(KEY_MAX_SIZE);
    }

    public void clearMaxSize() {
        setIntValue(KEY_MAX_SIZE, 0);
    }

    public void setAccess(String access) {
        setStringValue(KEY_ACCESS, access);
    }

    public String getAccess() {
        return getStringValue(KEY_ACCESS);
    }

    public void setSecret(String secret) {
        setStringValue(KEY_SECRET, secret);
    }

    public String getSecret() {
        return getStringValue(KEY_SECRET);
    }

    public void setPolicyHandle(String policyHandle) {
        setStringValue(KEY_POLICY_HANDLE, policyHandle);
    }

    public String getPolicyHandle() {
        return getStringValue(KEY_POLICY_HANDLE);
    }

    public void setPolicyExpiry(int expiry) {
        setIntValue(KEY_POLICY_EXPIRY, expiry);
    }

    public int getPolicyExpiry() {
        return getIntValue(KEY_POLICY_EXPIRY);
    }

    public void setPolicyMaxSize(int maxSize) {
        setIntValue(KEY_POLICY_MAX_SIZE, maxSize);
    }

    public int getPolicyMaxSize() {
        return getIntValue(KEY_POLICY_MAX_SIZE);
    }

    public void setPolicyMinSize(int minSize) {
        setIntValue(KEY_POLICY_MIN_SIZE, minSize);
    }

    public int getPolicyMinSize() {
        return getIntValue(KEY_POLICY_MIN_SIZE);
    }

    public void setPolicyPath(String policyPath) {
        setStringValue(KEY_POLICY_PATH, policyPath);
    }

    public String getPolicyPath() {
        return getStringValue(KEY_POLICY_PATH);
    }

    public void setPolicyContainer(String policyContainer) {
        setStringValue(KEY_POLICY_CONTAINER, policyContainer);
    }

    public String getPolicyContainer() {
        return getStringValue(KEY_POLICY_CONTAINER);
    }
}
