package io.filepicker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.filepicker.R;

import android.app.ActionBar;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.MenuItem;
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

	public static final String PREFS_NAME = "filepicker";

	private ListView listview;
	private GridView gridview;
	private AdapterView<? extends android.widget.Adapter> currentview = null;
	private String path = "/";
	private boolean saveas = false;
	private Uri fileToSave = null;
	private String mimetypes = "*/*";
	private final String TAG = "FilePickerActivity";
	private Uri imageUri = null; // for camera
	private String[] selectedServices = null;
	private String extension = "";
	private String displayName = null;
    private String parent_app_name = "FilePicker";

	private static final int CAMERA_REQUEST = 1888;

    public static void setParentAppName(String parentAppName) {
        parentAppName = parentAppName;
    }

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

		@Override
        // Sets view for every node in the list
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

	// Load a folder - which is basically like list of services or stuff included by them
	class FpapiTask extends AsyncTask<Long, Integer, Folder> {
		private AuthError authError = null;

		@Override
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
				e.printStackTrace();
				this.authError = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Folder result) {
			if (this.authError != null) {
				// Display auth activity
				Intent intent = new Intent(FilePicker.this, AuthActivity.class);
				intent.putExtra("service", this.authError.getService());
                intent.putExtra("parent_app", parent_app_name);
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

					@Override
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
								Intent intent;
                                if(hasImageCaptureBug()){
                                    Uri newImageUri = null;
                                    File path = new File(Environment.getExternalStorageDirectory().getPath() + "/Images");
                                    path.mkdirs();
                                    boolean setWritable = false;
                                    setWritable = path.setWritable(true, false);
                                    File file = new File(path, "Image_Story_" + System.currentTimeMillis() + ".jpg");
                                    newImageUri = Uri.fromFile(file);

                                    Log.i("Main Activity", "new image uri to string is " + newImageUri.toString());
                                    Log.i("Main Activity", "new image path is " + newImageUri.getPath());

                                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, newImageUri);
                                } else {
                                    String fileName = "" + System.currentTimeMillis() + ".jpg";
                                    ContentValues values = new ContentValues();
                                    values.put(MediaStore.Images.Media.TITLE, fileName);
                                    values.put(MediaStore.Images.Media.DESCRIPTION, "Image captured by camera");
                                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                                    imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                                }
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
                            setProgressVisible();
							int SDK_INT = android.os.Build.VERSION.SDK_INT;
							if (SDK_INT >= 11)
								currentview.setAlpha((float) 0.3);
							new PickFileTask().execute(inode.getPath());
						}
					}

				});
			}
		}
	}

    public boolean hasImageCaptureBug() {
        // list of known devices that have the bug
        ArrayList <String> devices = new ArrayList<String>();
        devices.add("android-devphone1/dream_devphone/dream");
        devices.add("generic/sdk/generic");
        devices.add("vodafone/vfpioneer/sapphire");
        devices.add("tmobile/kila/dream");
        devices.add("verizon/voles/sholes");
        devices.add("google_ion/google_ion/sapphire");

        return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/" + android.os.Build.DEVICE);
    }

	// When we select a file from the list
	class PickFileTask extends AsyncTask<String, Integer, FPFile> {
		private String fpurl;

		@Override
		protected FPFile doInBackground(String... arg0) {
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR");
				return null;
			}
			String path = arg0[0];
			try {
				return FilePickerAPI.getInstance().getLocalFileForPath(path,
						FilePicker.this);
			} catch (AuthError e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(FPFile result) {
			Intent resultIntent = new Intent();
			resultIntent.setData(Uri.parse("file://" + result.getLocalPath()));
			resultIntent.putExtra("fpurl", result.getFPUrl());
            resultIntent.putExtra("filename", result.getFilename());
			resultIntent.putExtra("fpfile", result);
			setResult(RESULT_OK, resultIntent);
			finish();
		}

	}

	class UploadLocalFileTask extends AsyncTask<Uri, Integer, FPFile> {
		private Uri uri;

		@Override
		protected FPFile doInBackground(Uri... uris) {
			// only one parameter may be passed
			if (uris.length != 1) {
				FilePickerAPI.debug("ERROR, too many urls passed as arguments");
				return null;
			}
			this.uri = uris[0];
			FilePickerAPI fpapi = FilePickerAPI.getInstance();
			try {
				return fpapi.uploadFileToTemp(uri, FilePicker.this);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(FPFile result) {
			Intent resultIntent = new Intent();
			resultIntent.setData(uri);
			if (result == null) {
				resultIntent.putExtra("fpurl", "");
			} else {
				resultIntent.putExtra("fpurl", result.getFPUrl());
                resultIntent.putExtra("filename", result.getFilename());
			}
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

			@Override
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
			if (arg0.length != 1) {
				FilePickerAPI.debug("ERROR");
				return "ERROR";
			}
			String path = arg0[0];
			try {
				FilePickerAPI.getInstance().saveFileAs(path, fileToSave,
						FilePicker.this);
			} catch (Exception e) {
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
        setProgressVisible();
		int SDK_INT = android.os.Build.VERSION.SDK_INT;
		if (SDK_INT >= 11)
			currentview.setAlpha((float) 0.3);
		Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
		new SaveFileTask().execute(path + "/" + filename);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //Check for API key and finish if not set
		if (!FilePickerAPI.isKeySet()) {
			Toast.makeText(this, "No Filepicker.io API Key set!",
					Toast.LENGTH_LONG).show();
			setResult(RESULT_CANCELED);
			finish();
		}

        // sessionCookie keeps request tokens from services like Dropbox and Facebook
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String sessionCookie = settings.getString("sessionCookie", "");

        // Creates singleton of FilePickerAPI and set session cookie
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

        //TODO PATCH! Only for app style reasons
        ActionBar actionBar = getActionBar();
        //parent_app_name = myIntent.getExtras().getString("parent_app");
        actionBar.setTitle(parent_app_name);
        actionBar.setDisplayHomeAsUpEnabled(true);

		if (path.equals("/")) {
            actionBar.setSubtitle("Please choose a file");
		} else {
			String[] splitPath = path.split("/");
			if (displayName != null) {
                actionBar.setSubtitle(displayName);
			} else {
				actionBar.setSubtitle(splitPath[splitPath.length - 1]);
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
					@Override
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
        }
        return super.onOptionsItemSelected(item);
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

                // TODO Patch
                Uri uri = null;
                if (hasImageCaptureBug()) {
                    File fi = new File("/sdcard/tmp");
                    try {
                        uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), fi.getAbsolutePath(), null, null));
                        if (!fi.delete()) {
                            Log.i("logMarker", "Failed to delete " + fi);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    uri = imageUri;
                }


				new UploadLocalFileTask().execute(uri);
				setProgressVisible();
				// enableLoading
			}
			break;
		}

	}

	private void setProgressVisible() {
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setVisibility(ProgressBar.VISIBLE);
	}
}
