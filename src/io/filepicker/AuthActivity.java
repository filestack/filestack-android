package io.filepicker;

import io.filepicker.R;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.support.v4.app.NavUtils;

public class AuthActivity extends Activity {
	private String service;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_picker_auth);
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent myIntent = getIntent();
		if (myIntent.getExtras().containsKey("service")) {
			service = myIntent.getExtras().getString("service");
			WebView webview = (WebView) findViewById(R.id.webView1);
			setTitle("Please Authenticate");
			webview.getSettings().setJavaScriptEnabled(true);
			webview.setWebViewClient(new WebViewClient() {
				//keep redirects in our app
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					return false; //false to handle redirects in the webview
				}
				
				public void onPageFinished(WebView view, String url) {
					ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
					progressBar.setVisibility(ProgressBar.INVISIBLE);
					
					if (url.startsWith(FilePickerAPI.FPBASEURL + "dialog")) {
						//load cookies
						setResult(RESULT_OK);
						AuthActivity.this.finish();
						overridePendingTransition(R.anim.right_slide_out_back,
								R.anim.right_slide_in_back);
						assert false : "shouldn't reach this point";
						return;
					}
				}
			});
			String url = FilePickerAPI.FPBASEURL + "api/client/" + service + "/auth/open";
			webview.loadUrl(url);
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.right_slide_out_back,
				R.anim.right_slide_in_back);
	}

}
