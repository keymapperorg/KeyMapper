--8<-- "go-to-settings.md"

## Automatically change the on-screen keyboard when a device connects/disconnects

!!! attention
    You must [grant Key Mapper WRITE_SECURE_SETTINGS permission](../adb-permissions/#write_secure_settings) for this to notification to be shown. Alternatively on Android 11+ you can just turn on the accessibility service.

!!! info
    This is restricted to Bluetooth devices in Key Mapper 2.2.0 and older. After updating to Key Mapper 2.3.0 you will need to set this up again because the data for the Bluetooth devices can't be migrated in a way that the new feature will work.

The last used Key Mapper keyboard will be automatically selected when a chosen device is connected. Your normal keyboard will be automatically selected when the device disconnects.

## Automatically change the on-screen keyboard when toggling key maps

!!! attention
    You must [grant Key Mapper WRITE_SECURE_SETTINGS permission](../adb-permissions/#write_secure_settings) for this to notification to be shown. Alternatively on Android 11+ you can just turn on the accessibility service.

The last used Key Mapper keyboard will be automatically selected when you unpause your key maps. Your normal keyboard will be automatically selected when they are paused.

## Toggle Key Mapper keyboard notification

!!! attention
    You must [grant Key Mapper WRITE_SECURE_SETTINGS permission](../adb-permissions/#write_secure_settings) for this to notification to be shown. Alternatively on Android 11+ you can just turn on the accessibility service.

This notification will select the last used Key Mapper keyboard if you are using your normal keyboard and will select your normal keyboard if the Key Mapper keyboard is being used.

## Automatically show keyboard picker (up to Android 9.0)

!!! attention
    This requires ROOT permission on Android 8.1 and Android 9.0 because Android blocked the ability for apps to show the input method picker when they are running in the background. Android removed the ability to show this even with ROOT on versions later than Android 9.0.

!!! info
    This is restricted to Bluetooth devices in Key Mapper 2.2.0 and older. After updating to Key Mapper 2.3.0 you will need to set this up again because the data for the Bluetooth devices can't be migrated in a way that the new feature will work.

When a device that you have chosen connects or disconnects the keyboard picker will show automatically. Choose the devices below.

## Key Mapper has root permission

!!! error "Don't turn this on if you don't know what 'rooting' is."
    Read more [here](https://en.wikipedia.org/wiki/Rooting_(Android)).

This setting needs to be turned on for some features in Key Mapper to work. You **must** grant Key Mapper root permission in your root management app (e.g Magisk, SuperSU) before you turn this on.

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
2. 
 --8<-- "go-to-settings.md"

3. Scroll down to the 'Workaround for Android 11 bug that sets the device id for input events to -1' setting and turn it on.
4. **To avoid confusion and headaches** read about how input methods work with Key Mapper [here](../quick-start.md#set-up-a-key-mapper-keyboard).
5. Tap 'Enable the Key Mapper keyboard' and turn on the Key Mapper GUI Keyboard or the Key Mapper Basic Input Method.
6. Tap 'Use the Key Mapper keyboard' and select the keyboard that you just enabled.
   It is recommended that you setup the setting to automatically change the on-screen keyboard when devices connect and disconnect. You can find this further up the page. This is useful if you don't want to use one of the Key Mapper keyboards all the time. If you want to change the keyboard manually then see [this](../faq.md#how-do-i-change-the-keyboard) question in the faq.

7. Connect the device that you want to fix to your Android device.
8. Tap 'Choose devices' and select the devices that should be fixed.
9. Your keyboard layout should be fixed! 🎉 If you're having issues checkout the FAQ below or [join](http://www.keymapper.club) the Discord server.

!!! faq
    - Can I use a non Key Mapper keyboard like Gboard instead? No because Key Mapper isn't allowed to tell other input methods what to type.
    - Can I add support for joysticks and triggers? No because Android doesn't allow apps to input motion events to other apps.