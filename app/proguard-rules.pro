# Add project specific ProGuard rules here.

# Keep annotation classes
-keep class javax.annotation.** { *; }
-keep class org.jetbrains.annotations.** { *; }
-keep class com.google.auto.value.** { *; }
-keep class com.google.code.findbugs.annotations.** { *; }

# Suppress warnings for annotation conflicts
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn com.google.auto.value.**
-dontwarn com.google.code.findbugs.annotations.**

# AppCrawler specific rules
-keep class com.google.android.appcrawler.** { *; }
-dontwarn com.google.android.appcrawler.**

# Markwon specific rules
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**
-keep class io.noties.prism4j.** { *; }
-dontwarn io.noties.prism4j.**

# OpenCV rules
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Krop library rules
-keep class com.attafitamim.krop.** { *; }
-dontwarn com.attafitamim.krop.**

# General rules for dependency conflicts
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.lang.model.**
-dontwarn com.sun.tools.javac.**

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep version information
-keep class * {
    public static final java.lang.String VERSION_NAME;
    public static final int VERSION_CODE;
}