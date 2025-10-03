# File: app/proguard-rules.pro
# Project: HomeoGO (Android, Jetpack Compose + Material3)
# Created: 03.okt.2025 07:40 (Rīga)
# ver. 1.0
# Purpose: R8/ProGuard keep rules for Azure Speech SDK (STT/TTS) and related reflection.
# Comments:
#  - New file (starts at ver. 1.0), safe to add even if minifyEnabled=false.
#  - Prevents stripping Speech SDK classes used via JNI/reflection.
#  - Includes common keeps for audio and OkHttp (used by many SDKs).
#  - Compose itself parasti neprasa keep; tooling/jvm debug daļu neieslēdzam šeit.

# 1. ---- Azure Speech SDK ------------------------------------------------------
-keep class com.microsoft.cognitiveservices.speech.** { *; }
-keep class com.microsoft.cognitiveservices.speech.audio.** { *; }
-keep class com.microsoft.cognitiveservices.speech.internal.** { *; }
-keep class com.microsoft.cognitiveservices.speech.util.** { *; }

# JNI bridges sometimes need broader keep:
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. ---- OkHttp/Okio (if SDK brings them in) ----------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 3. ---- Android Speech/Media (defensive) -------------------------------------
-dontwarn android.speech.**
-dontwarn android.media.**

# 4. ---- Kotlin/Coroutines (defensive minimal) --------------------------------
-dontwarn kotlinx.coroutines.**

# 5. ---- Compose (no generic keeps; rely on default shrinker rules) -----------
# If you later see missing classes in release, add specific keeps here.

# 6. ---- Logging (strip line numbers only if needed) --------------------------
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable
