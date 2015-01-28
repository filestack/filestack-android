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

    private Context context;

    private static PreferencesUtils prefUtils = null;

    private PreferencesUtils(Context context) {
        this.context = context;
    }

    public static PreferencesUtils newInstance(Context context) {
        if(prefUtils == null) {
            prefUtils = new PreferencesUtils(context);
        }

        return prefUtils;
    }

    public SharedPreferences getSharedPreferences(){
        return context.getSharedPreferences(KEY_PREFERENCES, context.MODE_PRIVATE);
    }

    // String
    public void setStringValue(String key, String value){
        getSharedPreferences().edit().putString(key, value).apply();
    }

    public String getStringValue(String key){
        return getSharedPreferences().getString(key, null);
    }

    // Boolean
    public void setBooleanValue(String key, Boolean value){
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }

    public Boolean getBooleanValue(String key){
        return getSharedPreferences().getBoolean(key, false);
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

    // Gets array of mimetypes and saves it as String
    public void setMimetypes(String[] mimetypes) {
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

    public void setAccess(String access) {
        setStringValue(KEY_ACCESS, access);
    }

    public String getAccess() {
        return getStringValue(KEY_ACCESS);
    }
}
