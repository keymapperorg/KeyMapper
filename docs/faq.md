This page serves to answer frequently asked questions about Key Mapper, and help solve any problems you may be facing.
See the [Quick Start Guide](quick-start.md) for more general help with using Key Mapper.

## "Can I remap joysticks/dpad/mouse buttons/mouse motion?"

No. Here is why:

Joysticks are complicated to remap because they are axes, so their inputs are a range depending on how much you press the stick. It would be a lot of work to integrate them into Key Mapper. Currently, the developer cannot implement this feature. See [here](https://github.com/keymapperorg/KeyMapper/blob/master/README.md) for more information. <!--- The developer has found a way that might be able to remap them. Therefore, you can consider this a planned feature. --->
<!--- as of end of 2022, early 2023, Key Mapper is no longer in development, and users should not be expecting features that will not come to fruition. --->

The D-pad is interpreted differently by Android to other keys. That makes it hard to remap. However, the developer has found a way that might be able to remap them. Therefore, you can consider this a planned feature.

Mouse buttons are not possible to remap with an accessibility service. There will never be support for them in this app. If other apps can do it, they do not work the same way Key Mapper does. If you want to remap mouse buttons, do not use Key Mapper.

Remapping the movement of the mouse isn't possible on unrooted Android. The developer doesn't know of a way to do this.

## "Key Mapper keeps randomly stopping/crashing/bugging/freezing"

Please follow the following steps:

- Turn off battery and memory optimisations in your device settings.
This is especially important for Huawei and Xiaomi devices, and devices with 2GB of RAM or less.
You can read about how to do this on your particular device at [dontkillmyapp.com](http://dontkillmyapp.com/)

- Reboot your device.
This is important for everyone to do if you are experiencing an issue with the app stopping. You must try rebooting your device. It works most of the time.

If you have tried BOTH steps and Key Mapper still stops working randomly, consider how you are using the app. If you have low-end hardware, and are playing a game while using repeating key maps, it's probably not a bug. It's probably just too much for your device.
If you think you are using Key Mapper in a reasonable way and still experience random crashes, ask about it in [the discord server.](http://keymapper.club)

## "How do I change the keyboard?"

There are multiple ways to do this.

- Tap the keyboard button at the bottom of your display when you are typing something. Not all devices have this and some devices have this feature hidden behind a setting somewhere.
- Tap the notification that allows you to change the keyboard when you are typing something. Not all devices have this.
- Open Key Mapper and tap "show input method picker" in the menu :material-menu:.

!!! tip
    If you want to switch between one of the Key Mapper keyboards and a normal keyboard when a device, such as a keyboard or game controller, connects/disconnects then Key Mapper can do this for you automatically. Set it up [here](user-guide/settings.md#automatically-change-the-on-screen-keyboard-when-a-device-connectsdisconnects).

## "My keyboard doesn't appear when using the app."

Key Mapper Basic Input Method (the keyboard built-in to the app) has no GUI/buttons. You can install the [Key Mapper GUI Keyboard](https://play.google.com/store/apps/details?id=io.github.sds100.keymapper.inputmethod.latin) to get a proper keyboard that can also perform Key Mapper actions.

## "Why don't my volume buttons work when I press them?"
Give the app Do Not Disturb access in your device settings. At the top of the homescreen in the Key Mapper app there is a "Fix" button to do this.

## "Why aren't the buttons on my Bluetooth device detected?"
Many Bluetooth devices (like headphones) aren't supported by Key Mapper out of the box. Bluetooth keyboards work most of the time. If you are willing and able to do some simple debugging, perhaps by installing utility apps or using ADB, [join the Discord server](http://keymapper.club) and we will try our best to get it working for you.

## "Why doesn't the app open in Dex mode?"
Samsung Dex doesn't allow apps with a 3rd party keyboard to open while in Dex mode. You can still configure key maps while out of Dex mode and your key maps which don't require a Key Mapper keyboard will still work in Dex mode.

If you *really* want to open the app in DeX, you can put DeX in developer mode:

- In Samsung DeX, open Settings -> Samsung DeX -> About Samsung DeX
- Click/Tap on the "Samsung DeX" title 5 times very fast
- You will see a pop-up "Turn on developer options?", press OK
- Restart Samsung DeX for the change to take effect

## "Why doesn't the app uninstall?"
You have probably enabled the app's device administrator in your device's settings. To uninstall the app, you must turn it off. The location of the device admin settings page varies on devices but on skins close to stock-Android it is under "Security" -> "Device admin apps". If you need help, [ask in the Discord server.](http://keymapper.club)

## "Why can't I remap some of my Joy Con buttons?"
Not all the Joy Con buttons are visible to accessibility services. There isn't anything the developer can do.

I tested the app on SwitchRoot Android and this is what works...

 **Working:**

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

**Not working:**

* Screenshot button
* All D-Pad buttons (used by OS)
* Both analog sticks (used by OS)
* Right analog stick button
* SR and SL (left joycon)
* SR and SL (right joycon)
* Power button

## "Add on-screen buttons to Key Mapper"

On-screen buttons are not a planned feature. In the near future, it will not be possible for apps to do this, even if some apps can right now. There is currently no work being done on adding on-screen buttons.

Google is removing the ability for apps to draw overlays over other apps. This is called the "system alert window" feature in the developer world. Android Go devices already can't do this.

## "A game doesn't detect my key maps"

Unsupported games
- Fortnite

There is no guarantee that your key maps will work in games. The app isn't designed for it.
Follow this guide if you are remapping buttons on a game controller to different buttons on a game controller.

- Connect your controller to your device.
- Create a trigger for whatever button you want to remap.
- Press "Add Action" and create a "Key Event" action by going to the "Key Event" tab.
- Choose your keycode AND select your controller as the device in the dropdown menu.
- Save the keymap and hopefully it will work.

## "How can I 'jump bridge' with Key Mapper? / How can I use Key Mapper for Minecraft PVP?"

This is not an app designed for gaming. Please look elsewhere for help with this, such as YouTube tutorials or ask in [the discord server.](http://keymapper.club) Also note that use of 'jump bridging' or very fast attacks on Minecraft servers where it is not allowed has absolutely nothing to do with the developer and it is not endorsed or encouraged. Again, this is not an app optimised for playing mobile games.

## "Can I use the app on Chromebook?"

Not all features work on Chromebook. The app is not supported on Chromebook.
Key Mapper can't "tap the screen" for obvious reasons. If you want to play a game on Chromebook using Key Mapper with keyboard controls (especially *Among Us*) then you are unlikely to be able to do so.

That being said, many features should work, such as remapping keyboard buttons.

## "Can I remap on-screen controls to buttons with Key Mapper?"

Technically yes but it would be a horrendous gaming experience. There is an action in Key Mapper to touch the screen but Key Mapper is only allowed to tap/swipe in one place at a time. Therefore, you wouldn't be able to tap forward on an on-screen joystick and shoot at the same time for example. This is how the Android API works and there is nothing I can do to change it.

[Ask for help :fontawesome-brands-discord:](http://keymapper.club){: .md-button .md-button--primary }

## "Can I remap my Bixby button?"

Key Mapper doesn't yet offer a way to remap the Bixby button but if you are rooted you can remap the Bixby button to something that Key Mapper can remap. Follow this [guide](https://forum.xda-developers.com/t/remap-bixby-manually-using-all-in-one-gestures-bind-to-flashlight-root.3660378/) on XDA.
