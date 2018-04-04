<p align="center"><img src="logo.svg" align="center" width="150"/></p>
<h1 align="center">Filestack Android SDK</h1>

<p align="center">
  <a href="https://bintray.com/filestack/maven/filestack-android">
    <img src="https://img.shields.io/badge/bintray-v5.0.0--0.2.0-blue.svg?longCache=true&style=flat-square">
  </a>
  <a href="https://filestack.github.io/filestack-android/">
    <img src="https://img.shields.io/badge/ref-javadoc-795548.svg?longCache=true&style=flat-square">
  </a>
  <img src="https://img.shields.io/badge/min_sdk-19_(4.4_kitkat)-green.svg?longCache=true&style=flat-square">
  <img src="https://img.shields.io/badge/target_sdk-26_(8.0_oreo)-green.svg?longCache=true&style=flat-square">
</p>

<p align="center">
  Android file uploader for Filestack. Upload local files or select from 10
  different cloud sources. Uploads from cloud sources transfer cloud to cloud,
  avoiding large mobile uploads. Supports Amazon Drive, Box, Dropbox, Facebook,
  GitHub, Gmail, Google Drive, Google Photos, Instagram, and OneDrive.
</p>

## Install
```gradle
implementation 'com.filestack:filestack-android:5.0.0-0.2.0'
```

## Demo
There's a sample project under the `demo` folder of this repo. Create and fill
out a credentials resource file before running.

src/main/res/values/keys.xml:
```xml
<resources>
    <string name="api_key">API_KEY</string>
    <string name="return_url">RETURN_URL</string>
    <string name="policy">POLICY</string>
    <string name="signature">SIGNATURE</string>
</resources>
```

## Setup

### Add a file provider for photos and videos
To enable users to take photos and videos within the picker, you need to define
a file provider for your app. This is required to avoid sending "file://" URI's
to the camera app, which will throw a FileUriExposedException on Android Nougat
and above. See the [google documentation][camera-docs] for more information.

Add a <provider> tag to your AndroidManifest.xml:
```xml
<provider
    android:name="android.support.v4.content.FileProvider"
    <!-- Change the authority to include your package name. -->
    android:authorities="com.filestack.android.demo.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

file_paths.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="pictures" path="Android/data/com.filestack.android.demo/files/Pictures" />
    <external-path name="movies" path="Android/data/com.filestack.android.demo/files/Movies" />
</paths>
```
We expect the "pictures" and "movies" names to be defined.

### Add an app link for cloud OAuth flows
To enable cloud sources, you must setup your app to be openable by URL. This is
part of the OAuth (login) flow for each cloud provider. We perform the OAuth
flow within the device's default browser (instead of a WebView) because it's a
security best practice. You can read more about the security of performing
OAuth in WebView's in this Google Developers [blog post][webview-oauth].

Setting this up requires three things: an intent filter to respond to the URL,
an entry activity that opens for the intent, and a configuration parameter when
launching the SDK. The intent filter is what tells the OS the app can be opened
by a URL, the entry activity is necessary to maintain a clear activity stack,
and the configuration parameter passes the URL to the Filestack API.

To avoid a disambiguation (app chooser) dialog during the OAuth flow, you will
need to verify your URL with Google. This (and more information about opening
an app by URL) is described in the Android documentation on
[App Links][app-links].

EntryActivity.java:
```java
public class EntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check to see if this Activity is the root activity
        if (isTaskRoot()) {
            // This Activity is the only Activity, so
            //  the app wasn't running. So start the app from the
            //  beginning (redirect to MainActivity)
            Intent mainIntent = getIntent(); // Copy the Intent used to launch me
            // Launch the real root Activity (launch Intent)
            mainIntent.setClass(this, MainActivity.class);
            // I'm done now, so finish()
            startActivity(mainIntent);
            finish();
        } else {
            // App was already running, so just finish, which will drop the user
            //  in to the activity that was at the top of the task stack
            finish();
        }
    }
}
```

Inside AndroidManifest.xml:
```xml
<activity android:name=".EntryActivity">

    <intent-filter android:label="@string/app_name">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- Accepts URIs beginning with "https://demo.android.filestack.com” -->
        <data android:scheme="https" android:host="demo.android.filestack.com" />
    </intent-filter>

</activity>
```

## Upload files

### Launch activity
```java
// Create an intent to launch FsActivity
Intent intent = new Intent(this, FsActivity.class);

// Create a config object with your account settings
// Using security (policy and signature) is optional
Config config = new Config("API_KEY", "RETURN_URL", "POLICY", "SIGNATURE");
intent.putExtra(FsConstants.EXTRA_CONFIG, config);

// Setting storage options is also optional
// We'll default to Filestack S3 if unset
// The Filename and MIME type options are ignored and overridden
StorageOptions storeOpts = new StorageOptions.Builder()
    .location("gcs")
    .container("android-uploads")
    .build();
intent.putExtra(FsConstants.EXTRA_STORE_OPTS, storeOpts);

// To manually handle uploading, set auto upload to false
// You can upload the user's selections yourself with the Client class
intent.putExtra(FsConstants.EXTRA_AUTO_UPLOAD, false);

// To customize the sources list, pass in a list of constants
// The sources will appear in the order you add them to the list
// Defaults to Camera, Device, Google Drive, Facebook, Instagram, and Dropbox
ArrayList<String> sources = new ArrayList<>();
sources.add(Sources.CAMERA);
sources.add(Sources.DEVICE);
sources.add(Sources.GOOGLE_DRIVE);
sources.add(Sources.GITHUB);
intent.putExtra(FsConstants.EXTRA_SOURCES, sources);

// Start the activity
startActivityForResult(intent, REQUEST_FILESTACK);
```

### Receive activity results
`FsActivity` returns immediately once a user selects files. The returned
response will always be an `ArrayList` of `Selection` objects. Receive them in
your calling activity like so:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_FILESTACK && resultCode == RESULT_OK) {
        Log.i(TAG, "received filestack selections");
        String key = FsConstants.EXTRA_SELECTION_LIST;
        ArrayList<Selection> selections = data.getParcelableArrayListExtra(key);
        for (int i = 0; i < selections.size(); i++) {
            Selection selection = selections.get(i);
            String msg = String.format(locale, "selection %d: %s", i, selection.getName());
            Log.i(TAG, msg);
        }
    }
}
```

### Receive upload status broadcasts
Because the actual uploading occurs in a background service, we need to
register a `BroadcastReceiver` to get a status and resultant `FileLink` for
each selection. When the picker returns to `onActivityResult()` you receive an
`ArrayList` of `Selection` objects. When an intent message is received in your
`BroadcastReceiver`, you will receive a status string, a `Selection` (matching  
one in the list), and a `FileLink` (if the upload succeeded). As the upload
progresses, the background service will also put up notifications about its
ongoing status.

UploadStatusReceiver.java:
```java
public class UploadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "UploadStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Locale locale = Locale.getDefault();
        String status = intent.getStringExtra(FsConstants.EXTRA_STATUS);
        Selection selection = intent.getParcelableExtra(FsConstants.EXTRA_SELECTION);
        FileLink fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);

        String name = selection.getName();
        String handle = fileLink != null ? fileLink.getHandle() : "n/a";
        String msg = String.format(locale, "upload %s: %s (%s)", status, name, handle);
        Log.i(TAG, msg);
    }
}
```

Register the receiver in your calling activity's `onCreate()`:
```java
// Be careful to avoid registering multiple receiver instances
if (savedInstanceState == null) {
    IntentFilter intentFilter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
    UploadStatusReceiver receiver = new UploadStatusReceiver();
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
}
```

## Native UI
At present this SDK doesn't offer many customization options, but the [Java
SDK][java-sdk] can be used to build a native UI. This SDK adds UI and
convenience on top of the Java SDK.

[app-links]: https://developer.android.com/training/app-links/index.html
[bintray]: https://bintray.com/filestack/maven/filestack-android
[camera-docs]: https://developer.android.com/training/camera/photobasics.html
[java-sdk-ref]: https://filestack.github.io/filestack-java/
[java-sdk]: https://github.com/filestack/filestack-java
[webview-oauth]: https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html
