-keep class com.splitwisepro.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.android.gms.** { *; }
