package io.filepicker.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.filepicker.R;
import io.filepicker.models.Node;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class Utils {

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



    // Show Toast
    public static void showQuickToast(Context context, int resId ){
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public static void showQuickToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // Get providers
    public static Provider[] getProviders(String[] providersCodes) {
        if(providersCodes == null)
            return Constants.providersList;

        ArrayList<Provider> selectedProviders = new ArrayList<Provider>();
        for(String code : providersCodes) {
            for (Provider provider : Constants.providersList) {
                if (provider.getCode().equals(code)) {
                    selectedProviders.add(provider);
                }
            }
        }

        return selectedProviders.toArray(new Provider[selectedProviders.size()]);
    }

    // Get providers which allows saving
    public static Provider[] getExportableProviders(String[] providerCodes) {
        ArrayList<Provider> exportableProviders = new ArrayList<Provider>();

        for(Provider provider : getProviders(providerCodes)) {
            if(provider.isExportSupported()) {
                exportableProviders.add(provider);
            }
        }

        return exportableProviders.toArray(new Provider[exportableProviders.size()]);
    }

    public static boolean isProvider(Node node) {
        boolean isProvider = false;

        for(int i = 0; i < Constants.providersList.length; i++ ) {
            if(node.getDisplayName().equals(Constants.providersList[i].getDisplayName())) {
                isProvider = true;
                break;
            }
        }

        return isProvider;
    }

    public static String getImageName() {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd MMMM yyyy hh:mm a");
        return "Image " + ft.format(date) + ".jpg";
    }
}
