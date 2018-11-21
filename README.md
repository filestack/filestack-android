<p align="center"><img src="logo.svg" align="center" width="100"/></p>
<h1 align="center">Filestack Android SDK</h1>

<p align="center">
  <a href="https://bintray.com/filestack/maven/filestack-android">
    <img src="https://img.shields.io/badge/bintray-v5.0.0--0.2.0-blue.svg?longCache=true&style=flat-square">
  </a>
  <a href="https://filestack.github.io/filestack-android/">
    <img src="https://img.shields.io/badge/ref-javadoc-795548.svg?longCache=true&style=flat-square">
  </a>
  <img src="https://img.shields.io/badge/min_sdk-19_(4.4_kitkat)-green.svg?longCache=true&style=flat-square">
  <img src="https://img.shields.io/badge/target_sdk-27_(8.1_oreo)-green.svg?longCache=true&style=flat-square">
</p>

<p align="center">
  Android file uploader for Filestack. Upload local files or select from 10
  different cloud sources. Uploads from cloud sources transfer cloud to cloud,
  avoiding large mobile uploads. Supports Amazon Drive, Box, Dropbox, Facebook,
  GitHub, Gmail, Google Drive, Google Photos, Instagram, and OneDrive.
</p>

## Install
```gradle
implementation 'com.filestack:filestack-android:5.1.0'
```

## Tester and Samples
To quickly test out the SDK you can clone this repo and build the development app (located in the `tester` directory). It contains settings UI to customize the picker and set credentials, no code changes necessary. You can also build one of the sample apps (located in the `samples` directory). The tester app is setup as a module of this project but the sample apps are setup as independent projects.

## Setup

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

## Upload files

### Use FilestackPicker - new way
`FilestackPicker.Builder` class has been introduced to simplify construction of Intents to run FsActivity.

```java
FilestackPicker picker = new FilestackPicker.Builder()
                    .config(...)
                    .storageOptions(...)
                    .config(...)
                    .autoUploadEnabled(...)                    
                    .sources(...)
                    .mimeTypes(...)
                    .multipleFilesSelectionEnabled(...)
                    .displayVersionInformation(...)
                    .build();
                    
picker.launch(activity); //use an Activity instance to launch a picker                                                          
```

### Receive activity results - new way
`FsActivity` returns immediately once a user selects files. The returned
response will always be of `List<Selections>` type. Receive the response in
the calling activity:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (FilestackPicker.canReadResult(requestCode, resultCode)) {
        Log.i(TAG, "received filestack selections");
        List<Selection> selections = FilestackPicker.getSelectedFiles(data);
        for (int i = 0; i < selections.size(); i++) {
            Selection selection = selections.get(i);
            String msg = String.format(locale, "selection %d: %s", i, selection.getName());
            Log.i(TAG, msg);
        }
    }    
}
```

### Launch activity - old way
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

// Restrict the types of files that can be uploaded
// Defaults to allowing all
String[] mimeTypes = {"application/pdf", "image/*", "video/*"};
intent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypes);

// Start the activity
startActivityForResult(intent, REQUEST_FILESTACK);
```

### Receive activity results - old way
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

### Receive upload status broadcasts
Because the actual uploading occurs in a background service, a `BroadcastReceiver` needs to be registered in order to get a status and resultant `FileLink` for each selected file. 
When the picker returns to `onActivityResult()` an `ArrayList` of `Selection` objects will be received.
When an intent message is received in the registered `BroadcastReceiver`, a status string, a `Selection` (matching  
one in the list), and a `FileLink` (if the upload succeeded) will be received. As the upload progresses, the background service will also put up notifications about its
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

## Native UI
At present this SDK doesn't offer many customization options, but the [Java
SDK][java-sdk] can be used to build a native UI. This SDK adds UI and
convenience on top of the Java SDK.

## Proguard
Filestack Android SDK definies its own `consumerProguardRule` to ensure that no additional configuration on consumer side is required.

## Deployment
_This is for Filestack devs._ Deployments are made to Bintray. You must have an account that's been added to the Filestack organization to deploy. Also make sure to follow general Filestack release guidelines. "BINTRAY_USER" and "BINTRAY_API_KEY" environment variables are required. To run:

```shell
export BINTRAY_USER=''
export BINTRAY_API_KEY=''
./gradlew bintrayUpload
```

[app-links]: https://developer.android.com/training/app-links/index.html
[bintray]: https://bintray.com/filestack/maven/filestack-android
[camera-docs]: https://developer.android.com/training/camera/photobasics.html
[java-sdk-ref]: https://filestack.github.io/filestack-java/
[java-sdk]: https://github.com/filestack/filestack-java
[webview-oauth]: https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html
