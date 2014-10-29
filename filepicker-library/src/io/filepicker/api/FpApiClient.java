package io.filepicker.api;

import android.content.Context;

import com.google.gson.Gson;

import java.util.Objects;

import io.filepicker.models.FPFile;
import io.filepicker.models.Folder;
import io.filepicker.models.UploadLocalFileResponse;
import io.filepicker.utils.PreferencesUtils;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
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
        RestAdapter restAdapter = null;

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

        // TODO when working check if / is needed

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
                "X-File-Name:tempfile.jpg",
                "Content-Type:application/octet-stream"
        })
        @POST(API_PATH_COMPUTER_URL)
        void uploadFile(
                @Query("js_session") String jsSession,
                @Body TypedFile file,
                Callback<UploadLocalFileResponse> response
        );

        @Headers("User-Agent: Mobile-Android")
        @GET(API_CLIENT_URL + "{provider}" + "/unauth")
        void logout(
                @Path("provider") String provider,
                @Query("js_session") String jsSession,
                Callback<Object> fpFile
        );

        @GET(API_PATH_URL)
        void getImage();

    }

    /** JsSession query param  */
    public static String buildJsSession(String apikey, String mimetypes) {
        Gson gson = new Gson();
        JsSession jsSession = new JsSession( new AppQuery(apikey, mimetypes));

        return gson.toJson(jsSession);
    }

    /** JsSession query param class */
    static class JsSession {
        AppQuery app;

        public JsSession(AppQuery app){
            this.app = app;
        }
    }

    /** AppQuery query param class */
    static class AppQuery {
        String apikey;
        String[] mimetypes;

        public AppQuery(String apikey, String mimetypes) {
            this.apikey = apikey;
            this.mimetypes = new String[]{mimetypes};
        }
    }
}
