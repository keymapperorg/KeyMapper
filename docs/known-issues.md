## Volume increases to max/minimum when pressing power and the respective volume button when the device is off

This seems to be a bug with accessibility services on Android Pie+ on some devices. It also happens with the Macrodroid app when you only enable their accessibility service which filters key events. I tested Key Mapper with no code in the onKeyEvent function in the accessibility service and it still happens. These are the devices that I've tested.

- :red-check: Rooted OxygenOS Android 10 OnePlus 6T
- :red-check: Rooted stock Android 9 Pie custom ROM on the OnePlus 6T
- :red-check: ​Non-rooted OxygenOS Android 10 OnePlus 7 Pro
- :green-cross: Android 10 on the Pixel 2