package io.filepicker.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

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

    private Utils() {}

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

    public static String getShortName(String name){
        return (name.length() > 25) ? name.substring(0, 25) + "..." : name;
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

        ArrayList<Provider> selectedProviders = new ArrayList<>();
        for(String code : providersCodes) {
            for (Provider provider : Constants.providersList) {
                if (provider.code.equals(code)) {
                    selectedProviders.add(provider);
                }
            }
        }

        return selectedProviders.toArray(new Provider[selectedProviders.size()]);
    }

    // Get providers which allows saving
    public static Provider[] getExportableProviders(String[] providerCodes) {
        ArrayList<Provider> exportableProviders = new ArrayList<>();

        for(Provider provider : getProviders(providerCodes)) {
            if(provider.exportSupported) {
                exportableProviders.add(provider);
            }
        }

        return exportableProviders.toArray(new Provider[exportableProviders.size()]);
    }

    public static boolean isProvider(Node node) {
        if(!(node instanceof Provider)) return false;

        boolean isProvider = false;

        for(int i = 0; i < Constants.providersList.length; i++ ) {
            if(node.displayName.equals(Constants.providersList[i].displayName)) {
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

    public static String filenameWithoutExtension(String filename) {
        return filename.replaceFirst("[.][^.]+$", "");
    }
}
