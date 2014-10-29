package io.filepicker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class PreferencesUtils {

    private static final String KEY_PREFERENCES = "io.filepicker.library";

    private static final String KEY_SESSION_COOKIE = "sessionCookie";
    private static final String KEY_MULTIPLE = "multiple";

    private Context context;

    private static PreferencesUtils prefUtils = null;

    public PreferencesUtils(Context context) {
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

    public void clearAll(){
        getSharedPreferences().edit().clear().commit();
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

    public void setMultiple() {
        setBooleanValue(KEY_MULTIPLE, true);
    }

    public boolean getMultiple() {
        return getBooleanValue(KEY_MULTIPLE);
    }
}
