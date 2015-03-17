Change Log
  * Max files option

Version 3.8.8 *(2015-03-09)
----------------------------
  * New: Security support

Version 3.8.7 *(2015-03-04)
----------------------------
  * Fix: Bug fixes

Version 3.8.6 *(2015-03-03)
----------------------------
  * Fix: Bug fixes

Version 3.8.5 *(2015-02-19)
----------------------------
  * New: When user specifies video mime type use it for Gallery and Camera
  * Fix: Code optimizations

Version 3.8.4 *(2015-02-09)
----------------------------
  * Fix: Update Retrofit, OkHttp and Picasso to the latest versions
  * Fix: Displaying images in Flickr

Version 3.8.3 *(2015-02-02)
----------------------------
  * Fix: Handle ActivityNotFoundExeception when Camera not found

Version 3.8.2 *(2015-01-30)
----------------------------
  * Fix: Clearing web view session for Android version < 21

Version 3.8.1 *(2015-01-28)
----------------------------
  * Fix: Bugs during uploading files
  * Fix: Login out from all services
  * Fix: Bugs with login view for services

Version 3.8.0 *(2015-01-13)
----------------------------
  * Fix: Major performance improvements
  * Fix: Bug fixing

Version 3.7.3 *(2015-01-09)
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
 