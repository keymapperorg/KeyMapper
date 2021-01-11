This page serves to answer frequently asked questions about Key Mapper, and help solve any problems you may be facing.
See the Quick Start Guide for more general help with using Key Mapper.

## "My Keyboard doesn't appear when using the app."

Key Mapper Basic Input Method (the keyboard built-in to the app) has no GUI/buttons. You can install the [Key Mapper GUI Keyboard](https://play.google.com/store/apps/details?id=io.github.sds100.keymapper.inputmethod.latin) to get a proper keyboard that can also perform Key Mapper actions.

## "Why don't my volume buttons work when I press them?"
Give the app Do Not Disturb access in your device settings. At the top of the homescreen in the Key Mapper app there is a "Fix" button to do this.

## "Why aren't my key maps being triggered?"
1. Disable all battery and memory optimisation features on your device. Consult https://dontkillmyapp.com for how to do this on your device.
2. Restart/reboot your device. This works in most cases.
3. Make sure your key maps are enabled. You can check in your notification tray, and unpause them by tapping the notification if necessary.
4. Restart the accessibility service by turning it off and on again. 
If none of these steps solve your problem, or the problem keeps coming back, [report an issue](https://github.com/sds100/KeyMapper/issues/new) or ask for help in [the Discord server](http://keymapper.club).

## "Why aren't the buttons on my Bluetooth device detected?"
Many Bluetooth devices (like headphones) aren't supported by Key Mapper out of the box. Bluetooth keyboards work most of the time. If you are willing and able to do some simple debugging, perhaps by installing utility apps or using ADB, [join the Discord server](http://keymapper.club) and we will try our best to get it working for you.

## "Why doesn't the app open in Dex mode?"
Samsung Dex doesn't allow apps with a 3rd party keyboard to open while in Dex mode. You can still configure key maps while out of Dex mode and your key maps which don't require a Key Mapper keyboard will still work in Dex mode.

## "Why doesn't the app uninstall?"
You have probably enabled the app's device administrator in your device's settings. To uninstall the app, you must turn it off. The location of the device admin settings page varies on devices but on skins close to stock-Android it is under "Security" -> "Device admin apps". If you need help, [ask in the Discord server.](http://keymapper.club)

## "Why aren’t the Keycode, Key or Text actions working?"
The Key Mapper keyboard must be enabled in your device settings and chosen as the active keyboard.
See Quick Start Guide for help.

## Why can't I remap some of my Joy Con buttons?
Not all the Joy Con buttons are visible to accessibility services. There isn't anything the developer can do.

We tested the app on SwitchRoot Android and this is what works...
#### Working:
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

#### Not working:
* Screenshot button
* All D-Pad buttons (used by OS)
* Both analog sticks (used by OS)
* Right analog stick button
* SR and SL (left joycon)
* SR and SL (right joycon)
* Power button

