## Can not unlock phone because there is no keyboard.

This has happened because you are using the Key Mapper Basic Input Method. This is an input method
with no on-screen keyboard. You will need to reboot your device into "safe mode". The way to do this
varies between devices so if these instructions don't work you will have to research how do to this
for your device online.

For most devices you must...

1. Turn on your device.
2. Hold down on the power button until the on-screen power menu pops up.
3. Hold down on the "power off" (or sometimes "restart") button in the power menu.
4. A popup should say whether you want to boot into safe mode. Do this.
5. Unlock your device after it has rebooted into safe mode.
6. Reboot your device into normal mode by pressing power off or restart in the power menu. You do **
   not** need to hold down on the "power off"/"restart" button to boot into safe mode.

## Key Mapper can't tap multiple places at the same time

If you are wanting to use Key Mapper to make buttons tap points on your screen then you need to use the "tap screen"
action. Due to a restriction in what background apps are allowed to do in Android, it is not possible for Key Mapper to
tap multiple places on screen at the same time.

## Key Mapper can't open the accessibility settings on some devices.

This is probably happening because your TV doesn't have any accessibility settings. The only way to fix it is to use
ADB (Android Debug Bridge) on a PC/Mac. Please follow
this [guide](user-guide/adb-permissions.md#enabling-the-accessibility-service) to grant enable the accessibility
service.

## External keyboard language is set to English US when using Key Mapper (Android 11 only)

This is a bug in Android 11 and should be fixed in Android 12. There is a setting in Key Mapper
2.3.0+ that helps you work around this issue. Read
more [here](https://keymapperorg.github.io/KeyMapper/redirects/android-11-device-id-bug-work-around).

## Key maps for an external device randomly stop working after a reboot

See issue [#783](https://github.com/keymapperorg/KeyMapper/issues/783).

**Problem**: Key Mapper uses the device id (a.k.a descriptor) to ensure triggers from specific
devices are only detected. The ids for some devices change after every reboot, which breaks this
filtering. This is not supposed to happen but fortunately it only happens very rarely.
**Solution**: Set the device for the trigger to "any device" so Key Mapper accepts the trigger from
any device.

## Volume increases to max/minimum when pressing power and the respective volume button when the device is off

This seems to be a bug with accessibility services on Android Pie+ on some devices. It also happens with the Macrodroid app when you only enable their accessibility service which filters key events. I tested Key Mapper with no code in the onKeyEvent function in the accessibility service and it still happens. These are the devices that I've tested.

- :red-check: Rooted OxygenOS Android 10 OnePlus 6T
- :red-check: Rooted stock Android 9 Pie custom ROM on the OnePlus 6T
- :red-check: Non-rooted OxygenOS Android 10 OnePlus 7 Pro
- :green-cross: Android 10 on the Pixel 2