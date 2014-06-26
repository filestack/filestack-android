This is sample File Picker Application for Android Studio.

This app is already configured to work with filepicker-library.

To run:
1. Download filepicker-library and sample-studio to the same folder.

You can check that the needed dependencies are added in:

a) settings.gradle

```java
include ':filepicker-library'
project(':filepicker-library').projectDir = new File(rootProject.projectDir, '../filepicker-library')
```

b) build.gradle (in dependencies section)

```java
compile project(':filepicker-library')
```

2. Set your FILEPICKER_API_KEY and PARENT_APP in MainActivity