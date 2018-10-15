# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/saten/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# filestack-java-specific rules
-keep public class com.filestack.internal.responses.** {
    private *;
    <init>(...);
}

-keep public class com.filestack.CloudResponse {
    private *;
    <init>(...);
}

-keep public class com.filestack.CloudItem {
    private *;
    <init>(...);
}

-keep public class com.filestack.AppInfo {
    private *;
    <init>(...);
}

# OkHttp-specific rules
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# Okio-specific rules
-dontwarn okio.**

-dontwarn com.squareup.okhttp.*
