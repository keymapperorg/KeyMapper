If your device is **rooted**, Key Mapper can grant itself these permissions by enabling the "Key Mapper has root permissions" toggle in the settings.

If your device is **not rooted** you must do the following.

## Prepare ADB (Android Debug Bridge)

1. Enable developer options on your device by going to device Settings -> About Phone and tapping Build Number many times until it says you've enabled developer options. The location of the Build Number may vary between devices.
2. Enable USB Debugging in developer options and plug your device into your PC.
3. Download the [Android SDK platform tools](https://developer.android.com/studio/releases/platform-tools.html) from here and unzip it.
4. Open a terminal/command prompt in the unzipped folder.
5. Type  or paste in a terminal: `adb devices` and your device should show up in the list after you click the prompt on your phone to allow USB debugging from your PC.

## WRITE_SECURE_SETTINGS

If you need more features of Key Mapper, you may grant the app permission to modify the secure settings on your device.

Type or paste in a terminal:

```
adb shell pm grant io.github.sds100.keymapper android.permission.WRITE_SECURE_SETTINGS
```

_For CI builds the package name is_ `io.github.sds100.keymapper.ci`.

More features which rely on this set of permission will come in the future. These permissions persist across reboots but need to be granted again if the app is reinstalled.

## Enabling the Accessibility Service

You may wish to enable Key Mapper's accessibility service using ADB if you cannot do so in-app due to manufacturer constraints.

To add Key Mapper to the List of the enabled accessibility providers, type or paste in a terminal:

```
adb shell settings put secure ENABLED_ACCESSIBILITY_SERVICES io.github.sds100.keymapper/io.github.sds100.keymapper.service.MyAccessibilityService
```