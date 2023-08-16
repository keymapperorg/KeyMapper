# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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
-keepclassmembers enum * { *; }

-keep class io.github.sds100.keymapper.** { *; }
-keep interface io.github.sds100.keymapper.** { *; }

-keep class com.google.android.material.** { *; }

-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }

-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

-keep class androidx.recyclerview.** { *; }
-keep interface androidx.recyclerview.** { *; }

-keep class android.hardware.input.IInputManager$Stub { *; }
-keep class android.content.pm.IPackageManager$Stub { *; }
-keep class android.permission.IPermissionManager$Stub { *; }

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations
-dontnote kotlinx.serialization.SerializationKt

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class io.github.sds100.keymapper.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class io.github.sds100.keymapper.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class io.github.sds100.keymapper.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type