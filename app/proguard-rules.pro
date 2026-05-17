# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes Signature
-keep class com.bilimusic.app.data.model.** { *; }
-keep class com.bilimusic.app.domain.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ExoPlayer
-dontwarn androidx.media3.**
