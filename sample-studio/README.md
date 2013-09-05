This is sample File Picker Application for Android Studio.

1. Add folder libraries/ in the top level folder. (In the same level as settings.gradle).

2. Add project to include in settings.gradle.

```java
':libraries:filepicker-library'
```

3. Compile project in build.gradle. Add this line in the dependencies.

```java
compile project(':libraries:filepicker-library')
```

4. No activities are to added in the manifest. Just the permissions.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

5. Set your_api_key in the MainActivity.