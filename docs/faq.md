This page serves to answer frequently asked questions about Key Mapper, and help solve any problems you may be facing.
See the Quick Start Guide for more general help with using Key Mapper.


## "My Keyboard doesn't appear when using the app."
Key Mapper Basic Input Method (the keyboard built-in to the app) has no GUI/buttons. You can install the [Key Mapper GUI Keyboard](https://play.google.com/store/apps/details?id=io.github.sds100.keymapper.inputmethod.latin) to get a proper keyboard that can also perform Key Mapper actions.

## "Why don't my volume buttons work when I press them?"
Give the app Do Not Disturb access in your device settings. At the top of the homescreen in the Key Mapper app there is a "Fix" button to do this.

## "How do I give the app more permissions?"
If your device is **rooted**, Key Mapper can grant itself the permission by enabling the "Key Mapper has root permissions" toggle in the settings.

If your device is **not rooted** you may have to do the following...

### Prepare debug interface (ADB)
1. Enable developer options on your device by going to device Settings -> About Phone and tapping Build Number many times until it says you've enabled developer options. The location of the Build Number may vary between devices.
2. Enable USB Debugging in developer options and plug your device into your PC.
3. Download the [Android SDK platform tools](https://developer.android.com/studio/releases/platform-tools.html) from here and unzip it.
4. Open a terminal/command prompt in the unzipped folder.
5. Type `adb devices` and your device should show up in the list after you click the prompt on your phone to allow USB debugging from your PC.

### ENABLED_ACCESSIBILITY_SERVICES
You may wish to enable Key Mapper's accessibility service using ADB if you cannot do so in-app due to manufacturer constraints.

To add Key Mapper to the List of the enabled accessibility providers, type or paste in a terminal:
```
adb shell settings put secure ENABLED_ACCESSIBILITY_SERVICES io.github.sds100.keymapper/io.github.sds100.keymapper.service.MyAccessibilityService
```
### WRITE_SECURE_SETTINGS
If you need more features of Key Mapper, you may grant the app an entire set of permissions called [Settings.Secure](https://developer.android.com/reference/android/provider/Settings.Secure.html).

Type or paste in a terminal:
```
adb shell pm grant io.github.sds100.keymapper android.permission.WRITE_SECURE_SETTINGS
```
You can now choose the Key Mapper keyboard without going into the Android settings. [Ensure the accessibility service is enabled.](#enabled_accessibility_services)

_For CI builds the package name is_ `io.github.sds100.keymapper.ci`.

More features which rely on this set of permission will come in the future. These permissions persist across reboots but need to be granted again if the app is reinstalled.

### "Why aren't my key maps being triggered?"
1. Disable all battery and memory optimisation features on your device. Consult https://dontkillmyapp.com for how to do this on your device.
2. Restart/reboot your device. This works in most cases.
3. Make sure your key maps are enabled. You can check in your notification tray, and unpause them by tapping the notification if necessary.
4. Restart the accessibility service by turning it off and on again. 
If none of these steps solve your problem, or the problem keeps coming back, [report an issue](https://github.com/sds100/KeyMapper/issues/new) or ask for help in [the Discord server](http://keymapper.club).

### "Why aren't the buttons on my Bluetooth device detected?"
Many Bluetooth devices (like headphones) aren't supported by Key Mapper out of the box. Bluetooth keyboards work most of the time. If you are willing and able to do some simple debugging, perhaps by installing utility apps or using ADB, [join the Discord server](http://keymapper.club) and we will try our best to get it working for you.

### "Why doesn't the app open in Dex mode?"
Samsung Dex doesn't allow apps with a 3rd party keyboard to open while in Dex mode. You can still configure key maps while out of Dex mode and your key maps which don't require a Key Mapper keyboard will still work in Dex mode.

### "Why doesn't the app uninstall?"
You have probably enabled the app's device administrator in your device's settings. To uninstall the app, you must turn it off. The location of the device admin settings page varies on devices but on skins close to stock-Android it is under "Security" -> "Device admin apps". If you need help, [ask in the Discord server.](http://keymapper.club)

### "Why aren’t the Keycode, Key or Text actions working?"
The Key Mapper keyboard must be enabled in your device settings and chosen as the active keyboard.
See Quick Start Guide for help.

### Why isn’t the Key Mapper “choose keyboard” notification working?
On Android 8.1 and newer, ROOT permission is required since Google has blocked the ability for apps to open the input method picker outside of the app. Therefore, this feature is not available on these newer devices. If your device isn’t rooted, Use the Android system's “Change keyboard” notification instead by tapping somewhere you can type and pressing the notification in the notification drawer. If your version of Android doesn't show a notification then there might be an icon in your navigation bar.

### Volume increases to max/minimum when pressing power and the respective volume button when the phone is off
This seems to be a bug with accessibility services on Android Pie and newer for some devices. It happens with the Macrodroid app as well when you only enable their accessibility service which filters keyevents. I tested Key Mapper with no code in the onKeyEvent function in the accessibility service and it still happens. I've tested it on a rooted OOS Android 10 OnePlus 6T, rooted stock Android Pie custom ROM on the OnePlus 6t, non-rooted OOS Android 10 OnePlus 7 Pro. It doesn't seem to happen with Android 10 on the Pixel 2.

### Why can't I remap some of my Joy Con buttons?
Not all the Joy Con buttons are visible to accessibility services. There isn't anything the developer can do.

We tested the app on SwitchRoot Android and this is what works...
##### Working:
* ZL (Button L2)
* L (Button L1)
* \- (Select)
* \+ (Start)
* Left analog stick button (unknown keycode 0)
* ZR (Button R2)
* R (Button R1)
* X (Button X)
* Y (Button Y)
* B (Button B)
* A (Button A)
* Home (Home)
* Volume up (Vol up)
* Volume down (Vol down)

##### Not working:
* Screenshot button
* All D-Pad buttons (used by OS)
* Both analog sticks (used by OS)
* Right analog stick button
* SR and SL (left joycon)
* SR and SL (right joycon)
* Power button

### Supported keys for screen off triggers and constraint
This feature only works on rooted devices and Key Mapper version 2.1.0+.
Let the developer know about any keys you would also like to be supported.
* Volume Up
* Volume Down
* Headset button
