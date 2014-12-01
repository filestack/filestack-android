Change Log
==========

Version 3.3 *(2014-11-14)*
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
 