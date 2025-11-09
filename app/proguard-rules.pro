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

-keepclassmembers enum * {
 public static **[] values();
 public static ** valueOf(java.lang.String);
 }

-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Keep data entities so proguard doesn't break serialization/deserialization.
-keep class io.github.sds100.keymapper.data.entities.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class com.google.android.material.** { *; }

-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }

-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

-keep class androidx.recyclerview.** { *; }
-keep interface androidx.recyclerview.** { *; }

# Keep all the AIDL classes because they must not be ofuscated for the bindings to work.
-keep class android.hardware.input.IInputManager { *; }
-keep class android.hardware.input.IInputManager$Stub { *; }
-keep class android.content.pm.IPackageManager { *; }
-keep class android.content.pm.IPackageManager$Stub { *; }
-keep class android.permission.IPermissionManager { *; }
-keep class android.permission.IPermissionManager$Stub { *; }
-keep class io.github.sds100.keymapper.api.IKeyEventRelayService { *; }
-keep class io.github.sds100.keymapper.api.IKeyEventRelayService$Stub { *; }
-keep class io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback { *; }
-keep class io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback$Stub { *; }
-keep class com.android.internal.telephony.ITelephony { *; }
-keep class com.android.internal.telephony.ITelephony$Stub { *; }

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

-keep class io.github.sds100.keymapper.sysbridge.service.SystemBridge {
    public <methods>;
    native <methods>;
    static <methods>;
    <init>(...);
}

# Keep all AIDL interface classes and their methods
-keep class io.github.sds100.keymapper.sysbridge.ISystemBridge** { *; }
-keep class io.github.sds100.keymapper.sysbridge.IEvdevCallback** { *; }
-keep class io.github.sds100.keymapper.sysbridge.IShizukuStarterService** { *; }

-keepclassmembers class io.github.sds100.keymapper.sysbridge.shizuku.ShizukuStarterService {
    public <init>(...);
}

# Keep binder provider classes
-keep class io.github.sds100.keymapper.sysbridge.provider.** { *; }

# Keep classes accessed via reflection or from system services
-keep class io.github.sds100.keymapper.sysbridge.utils.** { *; }

# Keep native method signatures
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that might be accessed via ContentProvider
-keep class io.github.sds100.keymapper.sysbridge.** extends android.content.ContentProvider { *; }

# Keep parcelable classes used in AIDL
-keep class io.github.sds100.keymapper.common.models.EvdevDeviceHandle { *; }

# Keep all rikka.hidden classes and interfaces as they contain AIDL files
-keep class rikka.hidden.** { *; }
-keep interface rikka.hidden.** { *; }

# Keep Android system API classes and interfaces that rikka.hidden depends on
# android.app package classes
-keep class android.app.ActivityManagerNative { *; }
-keep class android.app.ActivityTaskManager$RootTaskInfo { *; }
-keep class android.app.ContentProviderHolder { *; }
-keep class android.app.IActivityManager** { *; }
-keep class android.app.IApplicationThread** { *; }
-keep class android.app.IProcessObserver** { *; }
-keep class android.app.ITaskStackListener** { *; }
-keep class android.app.IUidObserver** { *; }
-keep class android.app.ProfilerInfo { *; }

# android.content package classes
-keep class android.content.IContentProvider** { *; }
-keep class android.content.IIntentReceiver** { *; }

# android.content.pm package classes
-keep class android.content.pm.IPackageManager** { *; }
-keep class android.content.pm.IPackageInstaller** { *; }
-keep class android.content.pm.ILauncherApps** { *; }
-keep class android.content.pm.IOnAppsChangedListener** { *; }
-keep class android.content.pm.IPackageInstallerCallback** { *; }
-keep class android.content.pm.ParceledListSlice { *; }
-keep class android.content.pm.UserInfo { *; }

# android.hardware package classes
-keep class android.hardware.input.IInputManager** { *; }
-keep class android.hardware.display.IDisplayManager** { *; }
-keep class android.hardware.display.IDisplayManagerCallback** { *; }

# android.os package classes
-keep class android.os.BatteryProperty { *; }
-keep class android.os.IBatteryPropertiesRegistrar** { *; }
-keep class android.os.IDeviceIdleController { *; }
-keep class android.os.IDeviceIdleController** { *; }
-keep class android.os.IUserManager { *; }
-keep class android.os.IUserManager** { *; }
-keep class android.os.RemoteCallback** { *; }
-keep class android.os.ServiceManager { *; }

# android.view package classes
-keep class android.view.DisplayInfo { *; }
-keep class android.view.IWindowManager** { *; }

# android.permission package classes
-keep class android.permission.IPermissionManager** { *; }

# android.net package classes
-keep class android.net.wifi.IWifiManager** { *; }

# com.android.internal package classes
-keep class com.android.internal.app.IAppOpsActiveCallback** { *; }
-keep class com.android.internal.app.IAppOpsNotedCallback** { *; }
-keep class com.android.internal.app.IAppOpsService** { *; }
-keep class com.android.internal.policy.IKeyguardLockedStateListener** { *; }

# Keep all Android AIDL interfaces (they implement IInterface)
-keep class android.** implements android.os.IInterface { *; }

# Keep Android system service stubs and natives
-keep class android.**Native { *; }
-keep class android.**$Stub** { *; }
-keep class android.**$Proxy** { *; }

# Keep Android hidden/internal classes that might be accessed via reflection
-dontwarn android.app.ActivityManagerNative
-dontwarn android.app.ActivityTaskManager$**
-dontwarn android.app.ContentProviderHolder
-dontwarn android.app.IActivityManager**
-dontwarn android.app.IApplicationThread**
-dontwarn android.app.IProcessObserver**
-dontwarn android.app.ITaskStackListener**
-dontwarn android.app.IUidObserver**
-dontwarn android.app.ProfilerInfo
-dontwarn android.app.AppOpsManager$**
-dontwarn android.content.IContentProvider**
-dontwarn android.content.IIntentReceiver**
-dontwarn android.content.pm.ILauncherApps**
-dontwarn android.content.pm.IOnAppsChangedListener**
-dontwarn android.content.pm.IPackageInstallerCallback**
-dontwarn android.content.pm.ParceledListSlice
-dontwarn android.content.pm.UserInfo
-dontwarn android.content.pm.PackageManagerHidden
-dontwarn android.hardware.display.IDisplayManager**
-dontwarn android.hardware.display.IDisplayManagerCallback**
-dontwarn android.os.BatteryProperty
-dontwarn android.os.IBatteryPropertiesRegistrar**
-dontwarn android.os.IDeviceIdleController
-dontwarn android.os.IDeviceIdleController**
-dontwarn android.os.IUserManager
-dontwarn android.os.IUserManager**
-dontwarn android.os.RemoteCallback**
-dontwarn android.os.ServiceManager
-dontwarn android.os.UserHandle$**
-dontwarn android.view.DisplayInfo
-dontwarn android.view.IWindowManager**
-dontwarn com.android.internal.app.**
-dontwarn com.android.internal.policy.**