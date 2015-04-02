package io.filepicker.api;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.util.HashMap;

import io.filepicker.Filepicker;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.Utils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class FpApiClient {

    public static final String DIALOG_URL = "dialog.filepicker.io";
    public static final String DIALOG_ENDPOINT = "https://" + DIALOG_URL;
    public static final String API_PATH_URL = "/api/path/";
    public static final String API_CLIENT_URL = "/api/client/";
    public static final String API_PATH_COMPUTER_URL = "/api/path/computer";
    public static final String AUTH_OPEN_URL = "/auth/open";


    private static FpApiInterface fpApiInterface;

    public static FpApiInterface getFpApiClient( Context context ) {
        if (fpApiInterface == null) {
            setFpApiClient(context);
        }
        return fpApiInterface;
    }

    public static void setFpApiClient(Context context) {
        final PreferencesUtils prefs = PreferencesUtils.newInstance(context);
        RestAdapter restAdapter;

        if (prefs.getSessionCookie() != null) {
            // Build with cookie
            restAdapter = getCookieRestAdapter(prefs.getSessionCookie());
        } else {
            // Build without cookie
            restAdapter = getRestAdapter();
        }

        fpApiInterface = restAdapter.create(FpApiInterface.class);
    }

    public static RestAdapter getRestAdapter() {
        return new RestAdapter.Builder()
                .setEndpoint(DIALOG_ENDPOINT)
                .build();
    }

    public static RestAdapter getCookieRestAdapter(final String session) {
        return new RestAdapter.Builder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Cookie", "session=" + session);
                    }
                })
                .setEndpoint(DIALOG_ENDPOINT)
                .build();
    }

    public interface FpApiInterface {
        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{provider}")
        void getFolder(
                @Path("provider") String provider,
                @Query("format") String format,
                @Query("js_session") String jsSession,
                Callback<Folder> folder
                );

        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{filePath}")
        FPFile pickFile(
                @Path("filePath") String filePath,
                @Query("format") String format,
                @Query("js_session") String jsSession
        );

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_COMPUTER_URL)
        void uploadFile(
                @Header("X-File-Name") String filename,
                @Query("js_session") String jsSession,
                @Body TypedFile file,
                Callback<UploadLocalFileResponse> response
        );

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_URL + "{path}")
        void exportFile(@Path("path") String path,
                        @Query("js_session") String jsSession,
                        @Body TypedFile file,
                        Callback<FPFile> response);
    }

    public static String getJsSession(Context context) {
        return buildJsSession(Filepicker.getApiKey(), context);
    }

    /** JsSession query param  */
    public static String buildJsSession(String apikey, Context context) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        String[] mimetypes = PreferencesUtils.newInstance(context).getMimetypes();

        String res = gson.toJson(new JsSession(apikey, context));
        if(mimetypes == null) {
            return gson.toJson(new JsSession(apikey, context));
        } else {
            return gson.toJson(new MimetypeSession(apikey, mimetypes, context));
        }
    }

    /** JsSession query param class */
    static class JsBaseSession {
        final String storeLocation;
        final String storePath;
        final String storeContainer;
        final String storeAccess;
        final String apikey;
        final String version;
        final int maxSize;

        String policy;
        String signature;

        JsBaseSession(String apikey, Context context) {
            PreferencesUtils prefs = PreferencesUtils.newInstance(context);

            this.apikey=apikey;
            this.storeLocation = prefs.getLocation();
            this.storePath = prefs.getPath();
            this.storeContainer = prefs.getContainer();
            this.storeAccess = prefs.getAccess();
            this.version = "v1";
            this.maxSize = prefs.getMaxSize();

            setupSecurity(prefs);
        }

        private void setupSecurity(PreferencesUtils prefs) {
            String secretKey = prefs.getSecret();
            String[] policyCalls = prefs.getPolicyCalls();
            int expiry = prefs.getPolicyExpiry();

            if(secretKey == null || secretKey.isEmpty() || policyCalls == null || policyCalls.length == 0 || expiry <= 0) {
                return;
            }

            HashMap<String, Object> jsonPolicy = new HashMap<>();
            jsonPolicy.put("call", policyCalls);

            long fpExpiry = System.currentTimeMillis()/1000 + expiry;
            jsonPolicy.put("expiry", fpExpiry);

            String handle = prefs.getPolicyHandle();
            if(handle != null && !handle.isEmpty()) {
                jsonPolicy.put("handle",handle);
            }

            int maxSize = prefs.getPolicyMaxSize();
            if(maxSize > 0) {
                jsonPolicy.put("maxSize", maxSize);
            }

            int minSize = prefs.getPolicyMinSize();
            if(minSize > 0) {
                jsonPolicy.put("minSize", minSize);
            }

            String path = prefs.getPolicyPath();
            if(path != null && !path.isEmpty()) {
                jsonPolicy.put("path", path);
            }

            String container = prefs.getPolicyContainer();
            if(container != null && !container.isEmpty()) {
                jsonPolicy.put("container", path);
            }

            JSONObject json = new JSONObject(jsonPolicy);
            String json_tos = json.toString();

            this.policy = Base64.encodeToString(json_tos.getBytes(), Base64.NO_WRAP);

            try {
                this.signature = Utils.encodeHmac(secretKey, policy).toLowerCase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class JsSession extends JsBaseSession {
        final String mimetypes;

        JsSession(String apikey, Context context) {
            super(apikey, context);
            this.mimetypes = "";
        }
    }

    static class MimetypeSession extends JsBaseSession{
        final String[] mimetypes;

        MimetypeSession(String apikey, String[] mimetypes, Context context) {
            super(apikey, context);
            this.mimetypes = mimetypes;
        }
    }
}
