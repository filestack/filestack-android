package io.filepicker;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import io.filepicker.api.FpApiClient;
import io.filepicker.utils.SessionUtils;

/**
 * Created by maciejwitowski on 10/23/14.
 */
public class AuthFragment extends Fragment {

    private static final String AUTH_OPEN = "open/?auth=true";

    private String providerUrl;
    private WebView webViewAuth;
    private ProgressBar mProgressBar;

    public static AuthFragment newInstance(String providerUrl) {
        AuthFragment frag = new AuthFragment();

        Bundle args = new Bundle();
        args.putString(Filepicker.NODE_EXTRA, providerUrl);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        providerUrl = getArguments().getString(Filepicker.NODE_EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_auth, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBarAuth);
        webViewAuth = (WebView) rootView.findViewById(R.id.webViewAuth);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webViewAuth, true);
        }

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
        return FpApiClient.DIALOG_ENDPOINT + FpApiClient.API_CLIENT_URL + providerUrl + FpApiClient.AUTH_OPEN_URL;
    }

    Contract getContract() {
        return (Contract) getActivity();
    }

    private class AuthWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains(AUTH_OPEN)) {
               SessionUtils.setSessionCookie(getActivity());

               if (getContract() != null) {
                   getContract().proceedAfterAuth();
               }

               return true;
            } else {
                return false;
            }
        }
    }

    private void hideProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    public interface Contract {

        void proceedAfterAuth();

    }

}
