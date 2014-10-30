package io.filepicker.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.Toast;

import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import io.filepicker.R;
import io.filepicker.models.Node;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class Utils {

    public static final String MIMETYPE_IMAGE = "image/*";
    private static final String MIMETYPE_ALL = "*/*";

    // Check if there is internet connection
    public static Boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()){
            return true;
        } else {
            Utils.showQuickToast(context, R.string.no_internet);
            return false;
        }
    }

    public static Provider[] providersList = new Provider[]{
            new Provider("Gallery",       "Gallery",      MIMETYPE_IMAGE ,    R.drawable.glyphicons_008_film, false, "GALLERY"),
            new Provider("Camera",        "Camera",       MIMETYPE_IMAGE,     R.drawable.glyphicons_011_camera, false, "CAMERA"),
            new Provider("Facebook",      "Facebook",     MIMETYPE_IMAGE,     R.drawable.glyphicons_390_facebook, true, "FACEBOOK"),
            new Provider("Instagram",     "Instagram",    MIMETYPE_IMAGE,     R.drawable.instagram, true, "INSTAGRAM"),
            new Provider("Flickr",        "Flickr",       MIMETYPE_IMAGE,     R.drawable.glyphicons_395_flickr, true, "FLICKR"),
            new Provider("Picasa",        "Picasa",       MIMETYPE_IMAGE,     R.drawable.glyphicons_366_picasa, true, "PICASA"),
            new Provider("Dropbox",       "Dropbox",      MIMETYPE_ALL,       R.drawable.glyphicons_361_dropbox, true, "DROPBOX"),
            new Provider("Box",           "Box",          MIMETYPE_ALL,       R.drawable.glyphicons_154_show_big_thumbnails, true, "BOX"),
            new Provider("Gmail",         "Gmail",        MIMETYPE_ALL,       R.drawable.glyphicons_399_email, false, "GMAIL"),
            new Provider("Github",        "Github",       MIMETYPE_ALL,       R.drawable.glyphicons_381_github, false, "GITHUB"),
            new Provider("Google Drive",  "GDrive",       MIMETYPE_ALL,       R.drawable.gdrive, false, "GOOGLE_DRIVE")
    };

    // Show Toast
    public static void showQuickToast(Context context, int resId ){
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    // Get providers
    public static Provider[] getProviders(String[] providersCodes) {
        if(providersCodes == null)
            return providersList;

        ArrayList<Provider> selectedProviders = new ArrayList<Provider>();
        for(String code : providersCodes) {
            for (Provider provider : providersList) {
                if (provider.getCode().equals(code)) {
                    selectedProviders.add(provider);
                }
            }
        }

        return selectedProviders.toArray(new Provider[selectedProviders.size()]);
    }

    public static boolean isProvider(Node node) {
        boolean isProvider = false;

        for(int i = 0; i < providersList.length; i++ ) {
            if(node.getDisplayName().equals(providersList[i].getDisplayName())) {
                isProvider = true;
                break;
            }
        }

        return isProvider;
    }

}
