# Garder les classes Sunmi SDK
-keep class com.sunmi.** { *; }
-keep interface com.sunmi.** { *; }

# Garder le bridge JS
-keep class com.kapdatalabs.kamo.MainActivity$SunmiPrintBridge { *; }
-keepclassmembers class com.kapdatalabs.kamo.MainActivity$SunmiPrintBridge {
    @android.webkit.JavascriptInterface <methods>;
}
