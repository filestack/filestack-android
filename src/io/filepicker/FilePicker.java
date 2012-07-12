package io.filepicker;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.filepicker.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FilePicker extends Activity {
	public static final String SAVE_CONTENT = "SAVE_CONTENT";

	private static final String PREFS_NAME = "filepicker";

	private ListView listview;
	private GridView gridview;
	private AdapterView<? extends android.widget.Adapter> currentview = null;
	private String path = "/";
	private boolean saveas = false;
	private Uri fileToSave = null;
	private String mimetypes = "*/*";
	private String TAG = "FilePickerActivity";
	private Uri imageUri = null; // for camera
	private String[] selectedServices = null;
	private String extension = "";
	private String displayName = null;

	private static final int CAMERA_REQUEST = 1888;

	class ThumbnailLoaderDataHolder {
		public final String url;
		public final ImageView imgv;

		public ThumbnailLoaderDataHolder(ThumbnailView imgv, String url) {
			this.url = url;
			this.imgv = imgv;
		}
	}

	class FPInodeView extends LinearLayout {
		public FPInodeView(Context context, Inode inode, boolean thumbnail) {
			super(FilePicker.this);
			setMinimumHeight(96);
			setOrientation(LinearLayout.HORIZONTAL);
			setGravity(Gravity.CENTER_VERTICAL);

			TextView textView = new TextView(FilePicker.this);
			textView.setTextSize(18.0f);

			textView.setText(inode.getDisplayName());

			ImageView icon = new ImageView(FilePicker.this);
			icon.setImageResource(inode.getImageResource());
			icon.setPadding(14, 18, 12, 16);
			addView(icon);

			if (inode.isDisabled())
				textView.setTextColor(Color.GRAY);
			if (thumbnail) {
				textView.setTextColor(Color.WHITE);
				icon.setColorFilter(0xffffffff, Mode.XOR);
			}

			addView(textView);

			if (inode.getIsDir()) {
				FilePickerAPI.getInstance()
						.precache(inode.getPath(), mimetypes);
			}
		}

		@Override
		public void onWindowSystemUiVisibilityChanged(int visible) {
			System.out.println("Visibility: visible");
		}
	}

	// Handle the listview
	class InodeArrayAdapter<T> extends ArrayAdapter<T> {
		private boolean thumbnail = false;

		public InodeArrayAdapter(Context context, int textViewResourceId,
				T[] objects) {
			super(context, textViewResourceId, objects);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			Inode inode = (Inode) getItem(position);

			if (inode.getThumb_exists() && thumbnail) {
				ThumbnailView icon = new ThumbnailView(FilePicker.this, inode);
				return icon;
			} else if (thumbnail) {
				return new NonThumbnailGridBlockView(FilePicker.this, inode);
			} else {
				return new FPInodeView(FilePicker.this, inode, thumbnail);
			}
		}

		public void setThumbnail(boolean thumbnail) {
			this.thumbnail = true;
		}
	}

	// Load a new folder
	class FpapiTask extends AsyncTask<Long, Integer, Folder> {
		private AuthError authError = null;

		protected Folder doInBackground(Long... l) {
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			try {
				if (path.equals("/")) {
					Inode[] root;
					if (selectedServices == null)
						root = fpapi.getProvidersForMimetype(mimetypes, saveas);
					else
						root = fpapi
								.getProvidersForServiceArray(selectedServices);
					return new Folder(root, "list", "");
				} else {
					return fpapi.getPath(path, mimetypes);
				}
			} catch (AuthError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.authError = e;
			}
			return null;
		}

		protected void onPostExecute(Folder result) {
			if (this.authError != null) {
				// Display auth activity
				Intent intent = new Intent(FilePicker.this, AuthActivity.class);
				intent.putExtra("service", this.authError.getService());
				startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_AUTH);
				overridePendingTransition(0, 0);
			} else if (result == null) {
				Toast.makeText(FilePicker.this, "An unexpected error occured. Are you connected to a network?", Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED);
				finish();
			} else {
				ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
				progressBar.setVisibility(ProgressBar.INVISIBLE);
				InodeArrayAdapter<Inode> iarrayadapter = new InodeArrayAdapter<Inode>(
						FilePicker.this, 0, result.getInodes());
				if (!path.equals("/"))
					setTitle(result.getName());
				if (result.getView().equals("thumbnails")) {
					iarrayadapter.setThumbnail(true);
					gridview.setAdapter(iarrayadapter);
					currentview = gridview;
					gridview.setBackgroundColor(Color.BLACK);
					gridview.getRootView().setBackgroundColor(Color.BLACK);
				} else {
					listview.setAdapter(iarrayadapter);
					currentview = listview;
				}
				currentview.setVisibility(View.VISIBLE);
				currentview.setOnItemClickListener(new OnItemClickListener() {

					@SuppressLint("NewApi")
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						Inode inode = (Inode) (parent.getAdapter()
								.getItem(position));
						if (inode.isDisabled()) {
							Toast.makeText(FilePicker.this, "App doesn't support this file type", Toast.LENGTH_SHORT).show();
							return;
						}
						if (inode.getIsDir()) {
							// is a subdirectory
							if (inode.getDisplayName().equals("Gallery")) {
								Intent intent = new Intent(
										Intent.ACTION_GET_CONTENT);
								intent.setType("image/*").addCategory(
										Intent.CATEGORY_OPENABLE);
								startActivityForResult(
										intent,
										FilePickerAPI.REQUEST_CODE_GETFILE_LOCAL);
							} else if (inode.getDisplayName().equals("Camera")) {
								Intent intent = new Intent(
										MediaStore.ACTION_IMAGE_CAPTURE);
								// intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
								// android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
								try {
									imageUri = Uri.parse("file://"
											+ File.createTempFile("fpf",
													".jpg"));
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								intent.putExtra(MediaStore.EXTRA_OUTPUT,
										imageUri);
								startActivityForResult(intent, CAMERA_REQUEST);
							} else {
								Intent intent = new Intent(FilePicker.this,
										FilePicker.class);
								intent.putExtra("path", inode.getPath());
								intent.setType(mimetypes);
								intent.putExtra("display_name",
										inode.getDisplayName());
								if (saveas) {
									intent.setData(fileToSave);
									intent.setAction(SAVE_CONTENT);
									if (extension.length() > 0)
										intent.putExtra("extension", extension);
									startActivityForResult(intent,
											FilePickerAPI.REQUEST_CODE_SAVEFILE);
								} else {
									startActivityForResult(intent,
											FilePickerAPI.REQUEST_CODE_GETFILE);
								}
							}
							overridePendingTransition(R.anim.right_slide_in,
									R.anim.right_slide_out);
						} else if (!saveas) {
							ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
							progressBar.setVisibility(ProgressBar.VISIBLE);
							int SDK_INT = android.os.Build.VERSION.SDK_INT;
							if (SDK_INT >= 11)
								currentview.setAlpha((float) 0.3);
							new PickFileTask().execute(inode.getPath());
						}
					}

				});
				
				//broken		
//				if (path.equals("/")) {
//					currentview.setOnItemLongClickListener(new OnItemLongClickListener() {
//	
//						public boolean onItemLongClick(AdapterView<?> parent,
//								View view, int position, long id) {
//							// TODO Auto-generated method stub
//							Inode inode = (Inode) (parent.getAdapter()
//									.getItem(position));
//							System.out.println(inode.getDisplayName());
//							System.out.println(inode.getClass().toString());
//							if (Service.class.isInstance(inode)) {
//								System.out.println("IS SERVICE");
//								Service service = (Service) inode;
//								FilePicker.this.unauth(service);	
//							}
//							return true;
//						}
//					});
//				}
			}
		}
	}

	// File selected
	class PickFileTask extends AsyncTask<String, Integer, FPFile> {
		private String fpurl;

		@Override
		protected FPFile doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR");
				return null;
			}
			String path = arg0[0];
			try {
				return FilePickerAPI.getInstance().getLocalFileForPath(path,
						FilePicker.this);
			} catch (AuthError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(FPFile result) {
			Intent resultIntent = new Intent();
			resultIntent.setData(Uri.parse("file://" + result.getLocalPath()));
			resultIntent.putExtra("fpurl", result.getFPUrl());
			setResult(RESULT_OK, resultIntent);
			finish();
		}

	}

	class UploadLocalFileTask extends AsyncTask<Uri, Integer, String> {
		private Uri uri;

		protected String doInBackground(Uri... uris) {
			// only one parameter may be passed
			if (uris.length != 1) {
				FilePickerAPI.debug("ERROR, too many urls passed as arguments");
				return "";
			}
			this.uri = uris[0];
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			try {
				return fpapi.uploadFileToTemp(uri, FilePicker.this);
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
		}

		protected void onPostExecute(String result) {
			Intent resultIntent = new Intent();
			resultIntent.setData(uri);
			resultIntent.putExtra("fpurl", result);
			setResult(RESULT_OK, resultIntent);
			DataCache.getInstance().clearCache();
			finish();
		}
	}

	@SuppressLint("NewApi")
	protected void getCookiesFromBrowser() {
		String fpcookie = CookieManager.getInstance().getCookie(
				FilePickerAPI.FPHOSTNAME);
		Pattern regex = Pattern.compile("session=\"(.*)\"");
		Matcher match = regex.matcher(fpcookie);
		if (!match.matches())
			return;
		String cookieData = match.group(1);
		// HttpCookie cookie = new HttpCookie(
		// "session",
		// "kI9Uzii1UpDIJGzpkKylOYUbwL8=?_expires=STEzNDM2NDAzMTEKLg==&_id=UydoXHhjMlx4YjNceGZje1x4YWJceDgxXHhjOHRceGUyXHhkY1x4ZmI1XHhhZUZcbicKcDEKLg==&_permanent=STAxCi4=&arg_cache=UycnCi4=&auth_dropbox=KGxwMQpTJ3E4M3MweXljcjJseGd0cycKcDIKYVMnNWxzOGRuZm5tbXR5cnQ0JwpwMwphLg==&dropbox_request_token=Y2NvcHlfcmVnCl9yZWNvbnN0cnVjdG9yCnAxCihjb2F1dGgub2F1dGgKT0F1dGhUb2tlbgpwMgpjX19idWlsdGluX18Kb2JqZWN0CnAzCk50UnA0CihkcDUKUydzZWNyZXQnCnA2ClMncWJpanU4aXc4OXprcHd4JwpwNwpzUydrZXknCnA4ClMnMXd2dzA1emhtZDIwd2I5JwpwOQpzYi4=");
		FilePickerAPI.getInstance().setSessionCookie(cookieData);

		// save persistently
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("sessionCookie", cookieData);
		editor.commit();
	}

	protected void unauth(final Service service) {
		if (service.getServiceId().length() == 0)
			return; //local
		new AlertDialog.Builder(this)
		.setTitle("Logout")
		.setMessage("Log out of " + service.getDisplayName() + "?")
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				FilePickerAPI.debug("Starting unauth");
				new UnAuthTask().execute(service);
				
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	class UnAuthTask extends AsyncTask<Service, Integer, Void> {
		@Override
		protected Void doInBackground(Service... arg0) {
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR with unauth task arguments");
				return null;
			}
			Service service = arg0[0];
			FilePickerAPI.getInstance().unauth(service);
			return null;
		}
	}

	class SaveFileTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR");
				return "ERROR";
			}
			String path = arg0[0];
			try {
				FilePickerAPI.getInstance().saveFileAs(path, fileToSave,
						FilePicker.this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "ERROR";
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(FilePicker.this, "Saved succesfully!",
					Toast.LENGTH_SHORT).show();
			Intent resultIntent = new Intent();
			// resultIntent.setData(Uri.parse("file://" + result));
			// resultIntent.putExtra("filepath", result);
			setResult(RESULT_OK, resultIntent);
			finish();
		}

	}

	@SuppressLint("NewApi")
	public void save() {
		EditText editText = (EditText) findViewById(R.id.editText1);
		String filename = editText.getText().toString() + extension;
		if (filename.length() == 0) {
			Toast t = new Toast(this);
			t.setText("Must specify filename");
			t.show();
			return;
		}
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setVisibility(ProgressBar.VISIBLE);
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		if (SDK_INT >= 11)
			currentview.setAlpha((float) 0.3);
		Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
		new SaveFileTask().execute(path + "/" + filename);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!FilePickerAPI.isKeySet()) {
			Toast.makeText(this, "No Filepicker.io API Key set!",
					Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED);
			finish();
		}

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sessionCookie = settings.getString("sessionCookie", "");
		FilePickerAPI.getInstance().setSessionCookie(sessionCookie);

		Intent myIntent = getIntent();
		if (myIntent != null && myIntent.getExtras() != null) {
			if (myIntent.getExtras().containsKey("path")) {
				path = myIntent.getExtras().getString("path");
			}
			if (myIntent.getExtras().containsKey("services")) {
				selectedServices = myIntent.getExtras().getStringArray(
						"services");
			}
			if (myIntent.getExtras().containsKey("extension")) {
				extension = myIntent.getExtras().getString("extension");
			}
			if (myIntent.getExtras().containsKey("display_name")) {
				displayName = myIntent.getExtras().getString("display_name");
			}
		}
		if (path.equals("/")) {
			setTitle("Please choose a file");
		} else {
			String[] splitPath = path.split("/");
			if (displayName != null) {
				setTitle(displayName);
			} else {
				setTitle(splitPath[splitPath.length - 1]);
			}
		}

		CookieSyncManager.createInstance(this); // webview

		if (myIntent.getAction() != null
				&& myIntent.getAction().equals(SAVE_CONTENT)) {
			if (myIntent.getData() == null) {
				Log.e(TAG, "No data passed in intent");
				setResult(RESULT_CANCELED);
				finish();
			} else {
				saveas = true;
				fileToSave = myIntent.getData();
			}
		}

		if (myIntent.getType() != null)
			mimetypes = myIntent.getType();

		if (saveas) {
			setContentView(R.layout.activity_file_picker_saveas);
			Button saveButton = (Button) findViewById(R.id.button1);
			if (extension.length() > 0) {
				TextView textView = (TextView) findViewById(R.id.textView1);
				textView.setText(extension);
			}
			if (path.equals("/") || path.equals("/Facebook/")) {
				saveButton.setEnabled(false);
			} else {
				saveButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						FilePicker.this.save();
					}
				});
			}
			fileToSave = myIntent.getData();
		} else {
			setContentView(R.layout.activity_file_picker);
		}
		listview = (ListView) findViewById(R.id.listView1);
		listview.setVisibility(View.INVISIBLE);
		gridview = (GridView) findViewById(R.id.gridView1);
		gridview.setVisibility(View.INVISIBLE);

		new FpapiTask().execute(5L);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.right_slide_out_back,
				R.anim.right_slide_in_back);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FilePickerAPI.REQUEST_CODE_AUTH:
			if (resultCode == RESULT_OK) {
				getCookiesFromBrowser();
				new FpapiTask().execute(6L);
			} else {
				setResult(RESULT_CANCELED);
				finish();
			}
			break;
		case FilePickerAPI.REQUEST_CODE_GETFILE:
		case FilePickerAPI.REQUEST_CODE_SAVEFILE:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
				DataCache.getInstance().clearCache();
				finish();
			}
			break;
		case FilePickerAPI.REQUEST_CODE_GETFILE_LOCAL:
			if (resultCode == RESULT_OK) {
				// add in url
				new UploadLocalFileTask().execute(data.getData());
				// enableLoading()
				setProgressVisible();
			}
			break;
		case CAMERA_REQUEST:
			if (resultCode == RESULT_OK) {
				new UploadLocalFileTask().execute(imageUri);
				setProgressVisible();
				// enableLoading
			}
			break;
		}

	}

	private void setProgressInvisible() {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setVisibility(ProgressBar.INVISIBLE);
	}

	private void setProgressVisible() {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setVisibility(ProgressBar.VISIBLE);
	}
}
