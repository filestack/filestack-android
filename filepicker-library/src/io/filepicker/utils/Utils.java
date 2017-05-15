package io.filepicker.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.filepicker.models.Node;
import io.filepicker.models.Provider;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class Utils {

    private static final SimpleDateFormat DATE_SUFFIX_FORMAT = new SimpleDateFormat("_dd_MM_yyyy_HH_mm", Locale.ENGLISH);

    private Utils() {}

    public static String getShortName(String name) {
        return (name.length() > 25) ? name.substring(0, 25) + "..." : name;
    }

    // Show Toast
    public static void showQuickToast(Context context, int resId) {
        if (context != null) {
           showQuickToast(context, context.getString(resId));
        }
    }

    public static void showQuickToast(final Context context, final String message) {
        if (context != null) {
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static ArrayList<Node> getProvidersNodes(Context context, String[] selectedProviders, boolean exportableOnly) {
        ArrayList<Node> providersNodes = new ArrayList<>();

        boolean userChosenProviders = (selectedProviders != null && selectedProviders.length > 0);

        for (Provider provider : Constants.PROVIDERS_LIST) {
            boolean codeConditionMet = !userChosenProviders || providerSelected(provider, selectedProviders);
            boolean exportConditionMet = !exportableOnly || provider.exportSupported;

            if (codeConditionMet && exportConditionMet) {
                providersNodes.add(provider);
            }
        }

        return providersNodes;
    }

    private static boolean providerSelected(Provider provider, String[] selectedCodes) {
        if (provider == null || provider.code == null || selectedCodes == null) {
            return false;
        }

        for (String selectedCode : selectedCodes) {
            if (provider.matchedCode(selectedCode)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isProvider(Node node) {
        if (!(node instanceof Provider)) {
            return false;
        }

        for (Provider provider : Constants.PROVIDERS_LIST) {
            if (node.displayName.equals(provider.displayName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean belongsToImageOnlyProvider(Node node) {
        for (Provider provider : Constants.PROVIDERS_LIST) {
            if (provider.mimetypes.equals(Constants.MIMETYPE_IMAGE) && node.linkPath.contains(provider.linkPath)) {
                return true;
            }
        }
        return false;
    }

    public static String getUploadedFilename(String fileName) {
        String timeSuffix = DATE_SUFFIX_FORMAT.format(new Date());
        int extIndex = fileName.lastIndexOf(".");
        if (extIndex != -1) {
            String extension = fileName.substring(extIndex);
            return fileName.replace(extension, timeSuffix + extension);
        } else {
            return fileName + timeSuffix;
        }
    }

    public static String filenameWithoutExtension(String filename) {
        return filename.replaceFirst("[.][^.]+$", "");
    }

    public static File getCacheFile(Context context, String path) {
        return new File(context.getCacheDir(), "io_filepicker_library_" + path);
    }

    public static File getSDFile(String path) {

        File root = new File(Environment.getExternalStorageDirectory()
                + File.separator + "FileStack" + File.separator);
        root.mkdirs();

        return new File(root,path);
    }

    public static void clearCachedFiles(Context context) {
        for (File file : context.getCacheDir().listFiles()) {
            if (file.getName().contains(Constants.CACHED_FILES_PREFIX)) {
                file.delete();
            }
        }

        File root = new File(Environment.getExternalStorageDirectory()
                + File.separator + "FileStack" + File.separator);
        if(root.exists()) {
            if(root.listFiles() != null) {
                for (File file : root.listFiles()) {
                    file.delete();
                }
            }
        }
    }

    public static boolean isImage(String extension) {
        return extension.equals(Constants.EXTENSION_JPEG) ||
                extension.equals(Constants.EXTENSION_JPG) ||
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
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
