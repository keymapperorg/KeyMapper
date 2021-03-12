--8<-- "go-to-settings.md"

## Automatically change the on-screen keyboard when a device connects/disconnects.

## Key Mapper has root permission

## Workaround for Android 11 bug that sets the device id for input events to -1 (2.3.0+, Android 11+)

!!! info
    See the Android 11 bug on Google's issue tracker [here](https://issuetracker.google.com/issues/163120692).

    There is a bug on Android 11 that changes the device id of all input events (e.g pressing buttons, moving joysticks) to -1 when any accessibility services are enabled. This means that apps can't determine which kind of device an input
    event came from. This breaks keyboards that aren't using an American English keyboard layout because Android uses
    the device id of a key event to determine what character to type when a key is pressed. If the device id is -1 Android
    defaults to the American English keyboard layout.

!!! warning
    This feature **will not fix game controllers** because joysticks and triggers send *motion* events and not *key* events.

Steps to work around this bug...

1. Enable the Key Mapper accessibility service.
2. Connect the device that you want to fix to your Android device.
3. 
 --8<-- "go-to-settings.md"

4. Scroll down to the 'Workaround for Android 11 bug that sets the device id for input events to -1' section.
5. Tap 'Choose devices' and select the devices that should be fixed.
6. It is highly recommended to install the [Key Mapper GUI Keyboard](https://play.google.com/store/apps/details?id=io.github.sds100.keymapper.inputmethod.latin) so that you can have an on-screen keyboard while using this feature. The Basic Input Method built-in to Key Mapper doesn't show anything on-screen.
7. Tap 'Enable the Key Mapper keyboard' and turn on the Key Mapper GUI Keyboard or the Key Mapper Basic Input Method.
8. Tap 'Use the Key Mapper keyboard' and select the keyboard that you just enabled.
9. Your keyboard layout should be fixed! 🎉 If you're having issues checkout the FAQ below or [join](http://www.keymapper.club) the Discord server.

!!! tip
    Key Mapper has a setting to automatically change the on-screen keyboard when devices that specify connect and disconnect. This is useful if you don't want to use the Key Mapper GUI Keyboard all the time.

!!! faq
    - Can I use a non Key Mapper keyboard like Gboard instead? No because Key Mapper isn't allowed to tell other input methods what to type.
    - Can I add support for joysticks and triggers? No because Android doesn't allow apps to input motion events to other apps.