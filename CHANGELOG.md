Change Log
==========

Version 5.0.0-0.2.0 *(2018-03-19)*
----------------------------

### Bug Fixes
  * Fixes a crash that would occur when a user selected a file from the local device browser, after
    they had previously viewed a cloud source

Version 5.0.0-0.1.0 *(2018-01-18)*
----------------------------

### Notes
  * Using filestack-java v0.6.0
  * **Corrected version naming to 5.0.0-0.1.0 as 2.0.0-alpha.x was obviously
    wrong**
  * We're still appending a pre-release (0.1.0) identifier because the public
    interface may change somewhat, but we consider it usable, reasonably stable

### Breaking Changes
  * Selection class implements Parcelable interface instead of Serializable
  * As such the method used to retrieve selections from intents has changed
  * Selection instances may have a null path (because uri is set instead)
  * Selection instances may have a null uri (because path is set instead)
  * Several classes have been moved to the internal package
  * Anything inside the internal package should not be considered public API

### Bug Fixes
  * A notification is displayed at the beginning of a local upload, instead of
    the completion
  * A notification is displayed for failed uploads

### New Features
  * Select local files using the Storage Access Framework (Files app UI)
  * Expanded local file support, upload anything, not just photos and videos

Version 2.0.0-alpha.2 *(2017-11-20)*
----------------------------

  * New options and several bug fixes
  * FS-2208 Fix upload button, auth flow refresh, and local upload crash
    * Fix: Upload button not showing when it should on initial launch and in
      other circumstances
    * Fix: UI not refreshing after returning from auth flow
    * Fix: Crash when trying to upload multiple local items
    * Fix: Notifications not showing on Android Oreo
  * FS-2144 Save UI state on rotate
    * Fix: Various confusing losses of state and unnecessary data loading that
      occurred when the device was rotated.
    * Fix: Divider spacing in grid view mode
  * FS-2142 Customize which sources are enabled and shown
    * New: You can now pass an option that specifies which sources are shown in
      the nav drawer. If the option is omitted, a default set is used.
  * FS-1970 Select files without automatically uploading
    * New: You can now pass an option that prevents the picker from uploading
      files automatically. This way an integrating app can have more control
      over what and how files are uploaded. The Client class from the base java
      SDK can be used to perform the uploads manually.

Version 2.0.0-alpha.1 *(2017-11-01)*
----------------------------

  * First release of 2.0 update
  * Completely new codebase based on Filestack Java SDK

Version 4.0.2 *(2017-06-19)*
----------------------------
  * Fix: Fails on multi-image gallery upload

Version 4.0.1 *(2017-06-05)*
----------------------------
  * Fix: Activity not closing after upload

Version 4.0.0 *(2017-05-29)*
----------------------------
  * New: Build process with Bintray upload
  * Fix: Google OAuth without Webview

Version 3.9.10 *(2015-12-11)*
----------------------------
  * New: Add callback for uploading local files
  * Fix: Bug fixes

Version 3.8.17 *(2015-12-07)*
----------------------------
  * New: Add single-service scenario

Version 3.8.16 *(2015-12-07)*
----------------------------
  * Fix: Bug fixes

Version 3.8.15 *(2015-10-20)*
----------------------------
  * Fix: Bug fixes

Version 3.8.14 *(2015-05-20)*
----------------------------
  * Fix: Bug fixes styles

Version 3.8.13 *(2015-04-20)*
----------------------------
  * Fix: Encode file names before pick

Version 3.8.12 *(2015-04-09)*
----------------------------
  * New: Add showErrorToast option
  * New: Add public method to upload files without going showing the interface
  * Fix: Fixed crash of the app when user uploads picture from Gallery->Google Drive

Version 3.8.11 *(2015-04-02)*
----------------------------
  * New: Add maxSize option for pick

Version 3.8.10 *(2015-04-02)*
----------------------------
  * New: Support for Evernote and OneDrive

Version 3.8.9 *(2015-03-30)*
----------------------------
  * New: Max files option

Version 3.8.8 *(2015-03-09)*
----------------------------
  * New: Security support

Version 3.8.7 *(2015-03-04)*
----------------------------
  * Fix: Bug fixes

Version 3.8.6 *(2015-03-03)*
----------------------------
  * Fix: Bug fixes

Version 3.8.5 *(2015-02-19)*
----------------------------
  * New: When user specifies video mime type use it for Gallery and Camera
  * Fix: Code optimizations

Version 3.8.4 *(2015-02-09)*
----------------------------
  * Fix: Update Retrofit, OkHttp and Picasso to the latest versions
  * Fix: Displaying images in Flickr

Version 3.8.3 *(2015-02-02)*
----------------------------
  * Fix: Handle ActivityNotFoundExeception when Camera not found

Version 3.8.2 *(2015-01-30)*
----------------------------
  * Fix: Clearing web view session for Android version < 21

Version 3.8.1 *(2015-01-28)*
----------------------------
  * Fix: Bugs during uploading files
  * Fix: Login out from all services
  * Fix: Bugs with login view for services

Version 3.8.0 *(2015-01-13)*
----------------------------
  * Fix: Major performance improvements
  * Fix: Bug fixing

Version 3.7.3 *(2015-01-09)*
----------------------------
  * Fix: Bug fixing

Version 3.7.2 *(2014-12-15)*
----------------------------
  * UI: Disable providers list and show progress bar when the user picks file from gallery or camera
  * New: Add ActionBar Up caret

Version 3.7.1 *(2014-12-12)*
----------------------------
  * Fix: UI improvements for upload and export list views
  * Fix: Improve Grid views efficiency for lists containing many elements
  * Fix: Bug fixing

Version 3.6 *(2014-12-10)*
----------------------------
  * Fix: Optimize java code

Version 3.5 *(2014-12-09)*
----------------------------
  * New: Allow setting choose options

Version 3.4 *(2014-12-04)*
----------------------------
  * Fix: Fixes bug when user rotates the phone while camera is used

Version 3.3 *(2014-12-01)*
----------------------------
  * New: Add progress bar during an uploading of a local file.
  * New: Add localPath property to FPFile pointing to the file’s path on the device
  * Fix: Changed library theme to FilepickerTheme so the so the probability of name conflict is lower.


Versions 3.0-3.2 *(2014-11-14)*
----------------------------
  * New: Add option to get multiple files.
  * New: Simplify the way project can be included in other projects for Maven and Gradle users.
  * New: Add service - Amazon Cloud Drive.
  * New: Export action to upload local files to services (with checking whether the file is uploaded as a new one or an update)
  * New: Add a button to logout from service
  * Fix: Use Square’s Picasso library to load images (instead of error-prone processing of Bitmaps in AsyncTasks)
  * Fix: Use Square’s Retrofit library for network operations and handling of network-related errors (instead of troublesome android-built methods)
  * Fix: User GSON library for JSON processing instead of built-in Java methods
  * Fix: Use fragments to display views according to android best practices.
  * Fix: Use Greenrobot EventBus for communication between libraries components
  * Fix: Getting path to local file is changed to work on KitKat and higher versions.
  * Fix: Change the way services and mimetypes can be selected.
  * Fix: Uploading/downloading data to/from Filepicker API is moved to background service.
