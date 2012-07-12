filepicker-android v1.0
=======================

Android version of filepicker.  Allow your users to pull in their content from Dropbox, Facebook, and more!

For more info see https://www.filepicker.io

The library provides an activity that your app can spawn that allows the user to open and save files.


Including In Your Project
=========================

1. Include filepicker-android in your project as a library in eclipse.

   First import the filepicker-android folder into your eclipse workspace by going to File->Import and then selecting Android->Import Existing Android Code.
   
   Then right click on your project and go to Properties->Android and then press Add under the library group.

2. Make sure to have the following permission lines in your AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

3. Add the following lines to AndroidManifest.xml
```xml
<activity
    android:name="io.filepicker.FilePicker"
    android:label="@string/title_activity_file_picker" >
</activity>
<activity
    android:name="io.filepicker.AuthActivity"
    android:label="@string/title_activity_file_picker_auth" >
</activity>
```

Java
====

###Imports###
```java
import io.filepicker.FilePicker;
import io.filepicker.FilePickerAPI;
```

###Setting the API Key###
Before making any filepicker calls, set the api key like so
```java
FilePickerAPI.setKey(MY_API_KEY);
```

###Getting a File###
Start the activity like this
```java
startActivityForResult(new Intent(this, FilePicker.class), FilePickerAPI.REQUEST_CODE_GETFILE);
```
This behaves just like the GET_CONTENT intent.

The data of the returned intent is a uri to the local file.
The extra fpurl is a path to the file on Filepicker.io's servers.

Get the result like this
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != RESULT_OK)
            return;
    Uri uri = data.getData();
    System.out.println("File path is " + uri.toString());
    System.out.println("FPUrl: " + data.getExtras().getString("fpurl");
}
```

`intent.getData()` is a content uri to the file

`intent.getExtras().getString("fpurl")` is an http url to the file

###Saving a File###
Start the activity like this
```java
Uri uri  = Uri.fromFile("/tmp/android.txt"); //a uri to the content to save
Intent intent = new Intent(FilePicker.SAVE_CONTENT, uri, this, FilePicker.class);
intent.putExtra("extension", ".txt"); //optional, add an extension
startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_SAVEFILE);
```

###Options###
1. Service specifications
    ```java
    intent.putExtra("services", new String[]{FPService.DROPBOX, FPService.GALLERY, FPService.FACEBOOK});
    ```

    Other services:
    1. DROPBOX
    2. CAMERA
    3. GALLERY
    4. FACEBOOK
    5. BOX
    6. GITHUB
    7. GMAIL
    8. GDRIVE

2. Mimetype filtering

   Only allow the user to choose a file of the specified mimetype.
    ```java
    intent.setType("image/*");
    ```
    or
    ```java
    intent.setType("text/plain");
    ```

For more reading on intents check out http://developer.android.com/guide/components/intents-filters.html
