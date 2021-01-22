This page aims to explain how to remap fingerprint gestures.
For specific troubleshooting, [consult the FAQ.](../faq)

Where screenshots are necessary, this guide uses two devices to demonstrate.

1. OnePlus 7 Pro, Android 10, Dark theme, 1440p, tiny font size
2. Samsung J3 6, Android 5, Light theme, 720p, medium font size

App interface pictured: `keymapper-2.3.0-alpha-ci.778`

This guide is updated to match current stable release UI. [Report inaccuracies.](https://github.com/sds100/KeyMapper/issues/new)

## Disclaimer

On some devices, the fingerprint sensor can be remapped by Key Mapper. There are generally 3 categories of devices with varying degrees of support for this feature:

1. Some devices have a fingerprint sensor that can detect directional gestures (up, down, left, right swipes) and your device allows third party apps such as Key Mapper to access and remap these.
2. Some devices have a fingerprint sensor that can detect directional gestures (up, down, left, right swipes) but your device does not allow Key Mapper to access them.
3. Some devices have a fingerprint sensor that is not capable of detecting directional gestures. This generally includes in-screen fingerprint sensors.

If your device falls into categories 2 or 3, you cannot remap fingerprint gestures, and you shouldn't continue with this guide. Even if your device allows for directional fingerprint gestures for system use, they may not allow third party apps such as Key Mapper to access them and therefore there is nothing the developer of Key Mapper can do to help you.

If your device supports remapping directional fingerprint gestures, this guide is for you.

## Remapping fingerprint gestures

From the Key Mapper home screen, tab the 'Fingerprint' tab.

Here you can set actions for the 4 directional gestures. Tapping any one of them will bring you to the action assignment screen for that gesture and by tapping 'Add action' at the bottom of the screen you can assign the action. [Click here for an explanation of all the actions you can choose from.](#)

After choosing an action (or actions) you can press the save :fontawesome-solid-save: icon in the bottom right to save the mapping.

## Customising fingerprint gesture maps

If you would like to further customise the gesture map, you can tap the 3 dot menu :fontawesome-solid-ellipsis-v: to the right of the action and open the 'action settings'.

On this screen you can choose from the following options:

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Hold down until swiped again

This option allows for keycode actions to be 'held', much like you could hold down a physical keyboard key. The action will continue until the same gesture is performed a second time.

___

&nbsp;Delay before next action (ms)<br /> 
![](../images/ui-slider-default-light-450px.png)

This option takes effect if you have multiple actions in your gesture map. The slider sets the amount of time between this action and the next.

___

&nbsp;Action count<br /> 
![](../images/ui-slider-default-light-450px.png)

This option serves as a multiplier. If action count is equal to 1, when your gesture map is triggered, your action will be performed once. If it is equal to 5, the action will be performed 5 times, etc.

___

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Repeat until swiped again

When this is turned on, Key Mapper will execute your actions repeatedly. This is particularly useful when emulating a keyboard key press, where in most applications holding down the key would result in a repeating output. 

___

&nbsp;Repeat every... (ms)<br /> 
![](../images/ui-slider-default-light-450px.png)

This option sets the time period for your repeating action. If this is set to 200, your action will repeat every 200ms, or in terms of frequency, 5 times per second. Some people prefer to think about this setting as a repeat rate, commonly measured in 'clicks' per second or cps. To calculate the appropriate time period for this option from a desired frequency, use this equation:

```
Time period (ms) = 1000 / Frequency ('cps')
```
___

Tap 'Save' to close the menu, preserving changes.

## Special options

For extra customisation, tap the 'Constraints and more' tab at the top of the screen when editing or creating a gesture map.

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Vibrate

&nbsp;Vibrate duration (ms)<br /> 
![](../images/ui-slider-default-light-450px.png)

Tapping 'Vibrate' will cause your device to vibrate whenever your gesture map is triggered.

## Adding constraints

You can add special constraints to your gestures maps in order to customise when they are allowed to run.

To add a constraint fron the 'Constraints and more' tab, tap 'Add constraint'.

You can choose from the following options:

Choosing 'App in foreground' will allow you to restrict your gesture map to working only if your app of choice is the actively selected window, i.e. on screen and being interacted with. 'App not in foreground' will likewise restrict your gesture map to working only if your app of choice isn't in focus.

Choosing 'Bluetooth device is connected/disconnected' can restrict your gesture map to working only if a specific bluetooth device is connected/disconnected.

If you have [root permission](#), you can restrict your gesture maps to work only when the screen is on or off.

Choosing 'Orientation' can restrict your gesture map to working only when the device is set to specific screen orientation.

Make sure to save :fontawesome-solid-save: your gesture map after applying these changes.