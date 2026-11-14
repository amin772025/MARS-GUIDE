# Add project specific ProGuard rules here.
-keep class com.marsguide.vzd.domain.model.** { *; }
-keep class com.marsguide.vzd.data.remote.** { *; }
-keep class com.marsguide.vzd.data.local.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.squareup.moshi.** { *; }
-keepnames class com.squareup.moshi.**

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlinx.coroutines.** { *; }

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.runtime.ComposeNode { *; }
