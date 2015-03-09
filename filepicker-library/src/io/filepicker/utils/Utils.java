package io.filepicker.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.filepicker.models.Node;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class Utils {

    private Utils() {}

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
    public static ArrayList<Node> getProviders(String[] providersCodes) {
        if(providersCodes == null || !(providersCodes.length > 0)) {
            return getProvidersAsNodes();
        }

        ArrayList<Node> selectedProviders = new ArrayList<>();

        for (String code : providersCodes) {
            for (Provider provider : Constants.PROVIDERS_LIST) {
                if (provider.code.equals(code)) {
                    selectedProviders.add(provider);
                }
            }
        }

        return selectedProviders;
    }

    // Get providers which allows saving
    public static ArrayList<Node> getExportableProviders(String[] providersCodes) {
        if(providersCodes == null || !(providersCodes.length > 0)) {
            return getExportableProvidersAsNodes();
        }

        ArrayList<Node> exportableProviders = new ArrayList<>();

        for (Node node : getProviders(providersCodes)) {
            Provider provider = (Provider) node;
            if (provider.exportSupported) {
                exportableProviders.add(provider);
            }
        }

        return exportableProviders;
    }

    private static ArrayList<Node> getProvidersAsNodes() {
        ArrayList<Node> nodesList = new ArrayList<Node>();
        for(Provider provider : Constants.PROVIDERS_LIST) {
            nodesList.add(provider);
        }

        return nodesList;
    }

    private static ArrayList<Node> getExportableProvidersAsNodes() {
        ArrayList<Node> nodesList = new ArrayList<Node>();
        for(Provider provider : Constants.PROVIDERS_LIST) {
            if(provider.exportSupported) {
                nodesList.add(provider);
            }
        }

        return nodesList;
    }

    public static boolean isProvider(Node node) {
        if(!(node instanceof Provider)) return false;

        boolean isProvider = false;

        for(int i = 0; i < Constants.PROVIDERS_LIST.size(); i++ ) {
            if(node.displayName.equals(Constants.PROVIDERS_LIST.get(i).displayName)) {
                isProvider = true;
                break;
            }
        }

        return isProvider;
    }

    public static boolean belongsToImageOnlyProvider(Node node) {
        boolean belongsToImageOnlyProvider = false;
        for (Provider provider : Constants.PROVIDERS_LIST) {
            if (provider.mimetypes.equals(Constants.MIMETYPE_IMAGE) &&
                    node.linkPath.contains(provider.linkPath)) {

                belongsToImageOnlyProvider = true;
                break;
            }
        }

        return belongsToImageOnlyProvider;
    }

    public static String getUploadedFilename(String mimetype) {
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd MMMM yyyy hh:mm a");

        String basename = (mimetype.contains("video") ? "Video " : "Image ");
        String extension = (mimetype.contains("video") ? ".mp4" : ".jpg");
        return basename + ft.format(date) + extension;
    }

    public static String filenameWithoutExtension(String filename) {
        return filename.replaceFirst("[.][^.]+$", "");
    }

    public static File getCacheFile(Context context, String path) {
        return new File(context.getCacheDir(), "io_filepicker_library_" + path);
    }

    public static void clearCachedFiles(Context context) {
        for(File file : context.getCacheDir().listFiles()) {
            if(file.getName().contains(Constants.CACHED_FILES_PREFIX)) {
                file.delete();
            }
        }
    }

    public static boolean isImage(String extension) {
        return extension.equals(Constants.EXTENSION_JPEG) ||
                extension.equals(Constants.EXTENSION_JPG)  ||
                extension.equals(Constants.EXTENSION_PNG);
    }

    public static String encodeHmac(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return bytesToHex(sha256_HMAC.doFinal(data.getBytes()));
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
