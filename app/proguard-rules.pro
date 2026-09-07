-keep class androidx.media3.** { *; }
-keep class com.aether.player.** { *; }
-dontwarn androidx.media3.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
