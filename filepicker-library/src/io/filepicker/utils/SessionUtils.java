package io.filepicker.utils;
import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.filepicker.api.FpApiClient;

/**
 * Created by maciejwitowski on 1/27/15.
 */

// Setting, getting and clearing session cookie
public class SessionUtils {

    private SessionUtils(){}

    public static void setSessionCookie(Context context) {
         String sessionCookie = getSessionCookie();

        if(sessionCookie != null && !sessionCookie.isEmpty())
            PreferencesUtils.newInstance(context).setSessionCookie(sessionCookie);


        // Set FpApiClient which will use the cookie
        FpApiClient.setFpApiClient(context);
    }

    public static void clearSessionCookies(Context context) {
        // Remove cookie from prefs
        PreferencesUtils.newInstance(context).clearSessionCookie();

        // Remove cookies from CookieManager
        if(Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().removeAllCookies(null);
        } else {
            CookieSyncManager.createInstance(context);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
        }
        FpApiClient.setFpApiClient(context);
    }

    // Get session cookie from CookieManager
    public static String getSessionCookie() {
        String cookie = CookieManager.getInstance().getCookie(FpApiClient.DIALOG_URL);
        Pattern regex = Pattern.compile("session=\"(.*)\"");
        Matcher match = regex.matcher(cookie);
        if (!match.matches())
            return null;

        return match.group(1);
    }
}
