package io.filepicker.api;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.filepicker.Filepicker;
import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.PreferencesUtils;
import io.filepicker.utils.Utils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
    private static final int CONNECTION_TIMEOUT = 60;

    private static Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .create();
    private static FpApiInterface fpApiInterface;
    private static ExecutorService executor;


    public static FpApiInterface getFpApiClient(Context context) {
        if (fpApiInterface == null) {
            setFpApiClient(context);
        }
        return fpApiInterface;
    }

    public static void setFpApiClient(Context context) {
        final PreferencesUtils prefs = PreferencesUtils.newInstance(context);
        Retrofit restAdapter;

        executor = Executors.newCachedThreadPool();

        if (prefs.getSessionCookie() != null) {
            // Build with cookie
            restAdapter = getCookieRestAdapter(prefs.getSessionCookie());
        } else {
            // Build without cookie
            restAdapter = getRestAdapter();
        }

        fpApiInterface = restAdapter.create(FpApiInterface.class);
    }

    public static void cancelAll() {
        if (executor != null) {
                executor.shutdownNow();
                executor = null;
                fpApiInterface = null;
        }
    }

    public static Retrofit getRestAdapter() {
        return new Retrofit.Builder()
            .baseUrl(DIALOG_ENDPOINT)
            .callbackExecutor(executor)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(new OkHttpClient.Builder()
                .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .build())
            .build();
    }

    public static Retrofit getCookieRestAdapter(final String session) {
        OkHttpClient defaultHttpClient = new OkHttpClient.Builder()
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(
                new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request().newBuilder()
                            .addHeader("Cookie", "session=" + session)
                            .build();
                        return chain.proceed(request);
                    }
                }).build();

        return new Retrofit.Builder().client(defaultHttpClient)
            .baseUrl(DIALOG_ENDPOINT)
            .callbackExecutor(executor)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
    }

    public interface FpApiInterface {

        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{provider}")
        Call<Folder> getFolder(
                @Path("provider") String provider,
                @Query("format") String format,
                @Query("js_session") String jsSession);

        @Headers("User-Agent: Mobile-Android")
        @GET(API_PATH_URL + "{filePath}")
        Call<FPFile> pickFile(
                @Path("filePath") String filePath,
                @Query("format") String format,
                @Query("js_session") String jsSession);

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_COMPUTER_URL)
        Call<UploadLocalFileResponse> uploadFile(
                @Header("X-File-Name") String filename,
                @Query("js_session") String jsSession,
                @Body RequestBody file);

        @Headers({
                "User-Agent: Mobile-Android",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_URL + "{path}")
        Call<FPFile> exportFile(@Path("path") String path,
                                @Query("js_session") String jsSession,
                                @Body RequestBody file);
    }

    public static String getJsSession(Context context) {
        return buildJsSession(Filepicker.getApiKey(), context);
    }

    /** JsSession query param  */
    public static String buildJsSession(String apikey, Context context) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        String[] mimetypes = PreferencesUtils.newInstance(context).getMimetypes();

        if (mimetypes == null) {
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

            this.apikey = apikey;
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

            if (secretKey == null || secretKey.isEmpty() || policyCalls == null || policyCalls.length == 0 || expiry <= 0) {
                return;
            }

            HashMap<String, Object> jsonPolicy = new HashMap<>();
            jsonPolicy.put("call", policyCalls);

            long fpExpiry = System.currentTimeMillis()/1000 + expiry;
            jsonPolicy.put("expiry", fpExpiry);

            String handle = prefs.getPolicyHandle();
            if (handle != null && !handle.isEmpty()) {
                jsonPolicy.put("handle",handle);
            }

            int maxSize = prefs.getPolicyMaxSize();
            if (maxSize > 0) {
                jsonPolicy.put("maxSize", maxSize);
            }

            int minSize = prefs.getPolicyMinSize();
            if (minSize > 0) {
                jsonPolicy.put("minSize", minSize);
            }

            String path = prefs.getPolicyPath();
            if (path != null && !path.isEmpty()) {
                jsonPolicy.put("path", path);
            }

            String container = prefs.getPolicyContainer();
            if (container != null && !container.isEmpty()) {
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
