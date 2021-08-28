## Key Mapper randomly stops

Key Mapper has been killed while it has been in the background. You must follow these steps so that the problem can be solved super quickly! If you go to the developer without reading this then they will show you this same text again so read it **now**! 🙂

Here is how to fix it:

1. Many phone manufacturers (especially Xiaomi and Huawei) love to kill apps when they are running in the background. 😡 This is **not** the developer's fault and there is nothing they can do to fix it so don't leave a bad review when this happens. **You** might be able to fix it by turning off these app killing "features". Follow the amazing online guide at [https://dontkillmyapp.com](https://dontkillmyapp.com) that shows you how to turn this all off.

2. Turn the accessibility service off and on again and try to use Key Mapper like normal.

    There is a shortcut to do this if you see the image below at the top of the Key Mapper home screen. Tap "Fix" and then "Restart" in the pop up. Otherwise, you will have you to go find the "Accessibility" settings in your device's settings yourself.

       ![](images/home_error_key_mapper_killed.png)

3. You must **not** do the rest of these steps if this is the first time that you have completed step 1 and 2.

4. In some **very rare** cases there is a bug in Key Mapper that causes it to crash while it is running in the background. You will need to send a bug report so that the developer can investigate the issue. Tap "Report bug" in Key Mapper's home screen menu.

## External keyboard language is set to English US when using Key Mapper (Android 11 only)

This is a bug in Android 11 and should be fixed in Android 12. There is a setting in Key Mapper 2.3.0+ that helps you work around this issue. Read more [here](https://sds100.github.io/KeyMapper/redirects/android-11-device-id-bug-work-around).

## Volume increases to max/minimum when pressing power and the respective volume button when the device is off

This seems to be a bug with accessibility services on Android Pie+ on some devices. It also happens with the Macrodroid app when you only enable their accessibility service which filters key events. I tested Key Mapper with no code in the onKeyEvent function in the accessibility service and it still happens. These are the devices that I've tested.

- :red-check: Rooted OxygenOS Android 10 OnePlus 6T
- :red-check: Rooted stock Android 9 Pie custom ROM on the OnePlus 6T
- :red-check: Non-rooted OxygenOS Android 10 OnePlus 7 Pro
- :green-cross: Android 10 on the Pixel 2
