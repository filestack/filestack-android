# Filestack Android SDK

A UI and upload API for Filestack to upload files from a user's
local device or cloud services. This SDK provides a picker activity for easy
integration and a client class for manual control and customization.

The Filestack Android SDK supports Facebook, Instagram, Google Drive, Dropbox, Box, GitHub, Gmail, Google
Photos, Microsoft OneDrive, and Amazon Drive.

## Including In Your Project

```gradle
compile 'com.filestack:filestack-android:5.0.0-0.2.0'
```

## Usage

*For a working implementation of this project see the `demo` folder.*

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

### Add file provider for camera source
To enable users to take photos and videos within the picker, a file provider must be defined for the application to avoid sending "file://" URI's to the camera app. Failure to define a file provider will throw a FileUriExposedException
on Android Nougat and above. See the [google documentation][camera-docs] for
more information.

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

The "pictures" and "movies" names are expected to be defined.

### Setup for cloud authorization
To enable cloud sources, the app must be allowed to be opened by URL. This is part of the OAuth (login) flow for each cloud provider. The OAuth flow is performed within the device's default browser in accordance to OAuth's best security practices. More information about the security of performing OAuth in WebViews can be found in this Google Developers [blog post][webview-oauth].

Setting this up requires three things: an intent filter to respond to the URL, an entry activity that opens for the intent, and a configuration parameter when launching the SDK. The intent filter is what tells the OS the app can be opened by a URL, the entry activity is necessary to maintain a clear activity stack, and the configuration parameter passes the URL to the Filestack API.

To avoid a disambiguation (app chooser) dialog during the OAuth flow, the URL must be verified with Google. This (and more information about opening an app by URL) is described in the Android documentation on [App Links][app-links].

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
        <!-- Accepts URIs that begin with "https://demo.android.filestack.com” -->
        <data android:scheme="https" android:host="demo.android.filestack.com" />
    </intent-filter>

</activity>
```

## Receiving selected items
`FsActivity` returns immediately once a user selects files. The returned
response will always be an `ArrayList` of `Selection` objects. Receive the response in
the calling activity:

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

## Receiving upload status
Because the actual uploading occurs in a background service, a `BroadcastReceiver` and `FileLink` must be registered to get a status and resultant for each selection. When the picker returns to `onActivityResult()`an
`ArrayList` of `Selection` objects will be received. When an intent message is received in the
`BroadcastReceiver`, a status string, a `Selection` (matching  
one in the list), and a `FileLink` (if the upload succeeded) will be received. As the upload
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

Register the receiver in the calling activity's `onCreate()`:
```java
// Be careful to avoid registering multiple receiver instances
if (savedInstanceState == null) {
    IntentFilter intentFilter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
    UploadStatusReceiver receiver = new UploadStatusReceiver();
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
}
```

## Custom UI using client
The [base Java SDK][java-sdk] contains a `Client` class that can be used to
perform both local uploads and cloud integrations. To use the class, create
a config as mentioned above, then create the client with it. Additionally,
the client can be used to get all the same functionality without any of the
Filestack UI. Reference doc for the `Client` class can be found
[here][java-sdk-ref].

[screen-recording]: /demo/media/recording.gif
[app-links]: https://developer.android.com/training/app-links/index.html
[java-sdk]: https://github.com/filestack/filestack-java
[java-sdk-ref]: https://filestack.github.io/filestack-java/
[camera-docs]: https://developer.android.com/training/camera/photobasics.html
[webview-oauth]: https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html
