
filepicker-android
==================

Android version of Filepicker.  Allow your users to pull in their content from Dropbox, Facebook, and more!

For more info see https://www.filepicker.io

The library provides an activity that your app can spawn that allows the user to open and save files.

![Sample Screenshots][1]



Including In Your Project
=========================

This library is distributed as Android library project so it can be included by referencing it as a library project.

https://bintray.com/filestack/maven/filepicker-android

If you use Maven, you can include this library as a dependency:

```xml
<dependency>
  <groupId>io.filepicker</groupId>
  <artifactId>filepicker-android</artifactId>
  <version>4.0.1</version>
  <type>pom</type>
</dependency>
```

For Gradle users:

```xml
compile 'io.filepicker:filepicker-android:4.0.1'
```

ProGuard
========
Add the following to your ProGuard rules
````ProGuard
-keepattributes *Annotation*, Signature

##== Filepicker ==
-keep class io.filepicker.** { *; }

##== Retrofit ==
-keep class retrofit.** { *; }
-keepclassmembernames interface * {
    @retrofit.http.* <methods>;
}

##== Gson ==
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

````

Usage
=====

*For a working implementation of this project see the `sample-studio/` folder.*

###Setting api key###

Api key must be set before making any calls to Filepicker.
To set api key use

```java
Filepicker.setKey(MY_API_KEY).
```

###Setting application name###
If you want your application’s name to be visible at the top of Filepicker view, set it with

```java
Filepicker.setAppName(MY_APP_NAME)
```

###Getting a file###

To get a file, start Filepicker activity for result.

```java
Intent intent = new Intent(this, Filepicker.class);
startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
```

Then, receive the Filepicker object on activity result.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == Filepicker.REQUEST_CODE_GETFILE) {
      if(resultCode == RESULT_OK) {

        // Filepicker always returns array of FPFile objects
        ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);

        // Option multiple was not set so only 1 object is expected
        FPFile file = fpFiles.get(0);

        // Do something cool with the result
      } else {
        // Handle errors here
      }

    }
}
```

FpFile object contains following fields:

  * container - container in S3 where the file was stored (if it was stored)
  * url - file link to uploaded file
  * filename - name of file
  * localPath - local path of file
  * key - unique key
  * type - mimetype
  * size - size in bytes

All fields’ values can be retrieved using conventional java getters (i.e for field “size”, getter method “getSize()” is used).

###Getting multiple files###

```java
Intent intent = new Intent(this, Filepicker.class);
intent.putExtra("multiple", true);
```
###Choosing max files###

```java
Intent intent = new Intent(this, Filepicker.class);
intent.putExtra("maxFiles", 20);
```

###Choosing max file size###

```java
Intent intent = new Intent(this, Filepicker.class);
intent.putExtra(“maxSize”, 10*1024*1024);
```

###Store options###

```java
Intent intent = new Intent(this, Filepicker.class);
intent.putExtra("location", "S3");
intent.putExtra("path", "/example/123.png");
intent.putExtra("container", "example_bucket");
intent.putExtra("access", "public");
```

NOTE: Setting ‘path’ option disables generating unique key value for Filepicker file. When ‘path’ is used with ‘multiple’ option, all stored files will have the same key (which means they won’t be saved in storages like S3 which requires unique key values).

###Choosing services###

By default the following services are available (meaning of keys in brackets is described below):

  * Gallery (GALLERY)
  * Camera (CAMERA)
  * Facebook (FACEBOOK)
  * Amazon Cloud Drive (CLOUDDRIVE)
  * Dropbox (DROPBOX)
  * Box (BOX)
  * Gmail (GMAIL)
  * Instagram (INSTAGRAM)
  * Flickr (FLICKR)
  * Picasa (PICASA)
  * Github (GITHUB)
  * Google Drive (GOOGLE_DRIVE)
  * Evernote (EVERNOTE)
  * OneDrive (SKYDRIVE)

To use only selected services:

```java
Intent intent = new Intent(this, Filepicker.class);
String[] services = {"FACEBOOK", "CAMERA", "GMAIL"};
intent.putExtra("services", services);
```

###Choosing mimetypes###
To see only content files with specific mimetypes within services user:

```java
Intent intent = new Intent(this, Filepicker.class);
String[] mimetypes = {"image/*", "application/pdf"};
intent.putExtra("mimetype", mimetypes);
```

###Exporting file###

The library offer also a way to export files. It can be used to easily save a taken picture in cloud services.
Note: export works for the following cloud services: Cloud Drive, Dropbox, Box, Instagram, Flickr, Picasa, Evernote, Skydrive.

```java
Intent intent = new Intent()
                    .setAction(Filepicker.ACTION_EXPORT_FILE)
                    .setClass(this, Filepicker.class)
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setData(MY_FILE_URI);

startActivityForResult(intent, Filepicker.REQUEST_CODE_EXPORT_FILE);
```

### Security ###
The library support security options.

```java
Intent intent = new Intent(this, Filepicker.class);
intent.putExtra("app_secret", appSecret);
intent.putExtra("policy_calls", calls);
intent.putExtra("policy_handle", handle);
intent.putExtra("policy_expiry", expiry);
intent.putExtra("policy_max_size", maxSize);
intent.putExtra("policy_min_size", maxSize);
intent.putExtra("policy_path", policyPath);
intent.putExtra("policy_container", policyContainer);
```

### Error toasts ###
By default, whenever an error occurs during uploading/downloading files, there is toast message displayed.
This message can be disabled.

```java
intent.putExtra("showErrorToast", false);
```

### Upload local file API call ###
Local files can be uploaded using the library without making the user go through the library interface.

```java
Filepicker.uploadLocalFile(uriToLocalFile, context);
```

If the information about the success, error and progress of this operation is needed, the callback can be provided.

```java
final String url = "PUT PATH TO LOCAL FILE HERE - something like content://com.android.providers.media.documents/document/image%3A64";

Filepicker.uploadLocalFile(Uri.parse(url), this, new FilepickerCallback() {
    @Override
    public void onFileUploadSuccess(FPFile fpFile) {
      // Do something on success
    }

    @Override
    public void onFileUploadError(Throwable error) {
      // Do something on error
    }

    @Override
    public void onFileUploadProgress(Uri uri, float progress) {
      // Do something on progress
    }
});
```

If the callback is registered then the activity must unregister callbacks onDestroy

```java
@Override
protected void onDestroy() {
    Filepicker.unregistedLocalFileUploadCallbacks();
    super.onDestroy();
}
```

The set of arguments is the same as specified in [docs](https://developers.filepicker.io/docs/security/).
Note the “policy” prefix in options.

[1]: https://raw.github.com/Ink/filepicker-android/master/sample-studio/sample_screen.png

###Enabling APIs for Google Services###

1. In your web browser navigate to  https://console.developers.google.com and sign in with your Google account.
2. Select “Dashboard” and click on the “Create Project” button, creating a new project for your app (following Google’s steps).
3. Once the project is successfully created, click on your project name in the upper bar of the Google console. Select the “Credentials” tab on the left Menu.
4. Click on the “Create Credentials” button, and select “OAuth Client ID” in the sub-menu.
5. Select Android in the list and complete the required information based on your app. Click “Create” and close the confirmation popup.
6. Return to the Dashboard section and click on “Enable API”.
7. Depending on the services you want to include:
 - **For Google Drive and/or Google Photos**: select “Drive API” under “Google Apps APIs” and click “Enable”.
 - **For Gmail**: select “Gmail API” under “Google Apps APIs” and click “Enable”.
