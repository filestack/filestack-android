package io.filepicker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.filepicker.api.FpApiClient;
import io.filepicker.utils.PreferencesUtils;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class AuthFragment extends Fragment {

    public interface Contract {
        public void proceedAfterAuth();
    }

    String providerUrl;

    WebView webViewAuth;
    ProgressBar mProgressBar;

    private static final String AUTH_OPEN = "open/?auth=true";

    public static AuthFragment newInstance(String providerUrl) {
        AuthFragment frag = new AuthFragment();

        Bundle args = new Bundle();
        args.putString(Filepicker.NODE_EXTRA, providerUrl);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        providerUrl = getArguments().getString(Filepicker.NODE_EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_auth, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBarAuth);
        webViewAuth = (WebView) rootView.findViewById(R.id.webViewAuth);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        webViewAuth.getSettings().setJavaScriptEnabled(true);
        webViewAuth.setWebViewClient(new AuthWebviewClient());

        String url = buildServiceAuthUrl();
        webViewAuth.loadUrl(url);
        hideProgressBar();
    }

    private String buildServiceAuthUrl() {
        return FpApiClient.DIALOG_ENDPOINT + FpApiClient.API_CLIENT_URL +
                providerUrl + FpApiClient.AUTH_OPEN_URL;
    }

    public Contract getContract() {
        return (Contract) getActivity();
    }

    // Store session cookie
    private void setSessionCookie() {
        String sessionCookie = getSessionCookie();

        if(!sessionCookie.isEmpty())
            PreferencesUtils.newInstance(getActivity()).setSessionCookie(sessionCookie);


        // Set FpApiClient which will use the cookie
        FpApiClient.setFpApiClient(getActivity());
    }

    // Get session cookie from CookieManager
    private String getSessionCookie() {
        String cookie = CookieManager.getInstance().getCookie(FpApiClient.DIALOG_URL);
        Pattern regex = Pattern.compile("session=\"(.*)\"");
        Matcher match = regex.matcher(cookie);
        if (!match.matches())
            return null;

        return match.group(1);
    }

    private class AuthWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains(AUTH_OPEN)) {
               setSessionCookie();
               getContract().proceedAfterAuth();
               return true;
            } else {
                return false;
            }
        }
    }

    private void hideProgressBar() {
        if(mProgressBar != null)
            mProgressBar.setVisibility(View.GONE);
    }
}
