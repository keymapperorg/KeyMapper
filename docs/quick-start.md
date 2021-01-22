This page aims to give users an introduction to the Key Mapper interface and a demonstration on how to perform typical tasks with the app.
For specific troubleshooting, [consult the FAQ.](../faq)

!!! info
    Where screenshots are necessary, this guide uses two devices to demonstrate.

    1. OnePlus 7 Pro, Android 10, Dark theme, 1440p, tiny font size
    2. Samsung J3 6, Android 5, Light theme, 720p, medium font size

    App interface pictured: `keymapper-2.3.0-alpha-ci.778`

    This guide is updated to match current stable release UI. [Report inaccuracies.](https://github.com/sds100/KeyMapper/issues/new)

## First time setup

When you open Key Mapper, you will be presented with this screen.
This screen is referred to as the Key Mapper home screen.

![](images/hg-home-empty.png) ![](images/hg-home-empty-light.png)

Tapping the FIX button next to the accessibility service prompt will allow you to grant Key Mapper permission to run its accessibility service. You must do this for the app to work at all.

![](images/hg-warn-service.png)

You can then tap Key Mapper in your list of available services. There may be many others on this screen and your interface may be slightly different.

!!! warning
    If nothing happens when you tap FIX, or if you can't enable the service, see [this page](#) or ask for help in the [Discord server.](http://keymapper.club)

![](images/hg-settings-services.png) ![](images/hg-settings-services-light.png)

Tapping the FIX button next to the input method prompt will allow you to enable a Key Mapper compatible keyboard. If you want to perform actions like pressing keyboard keys or entering text you must enable this.

![](images/hg-warn-input-method.png)

If you have only installed the Key Mapper app, your list of available keyboards will contain "Key Mapper Basic Input Method" only. If you aim to use a physical keyboard (not on-screen) when using Key Mapper, this will be suitable for you.

!!! tip
    If you want an on-screen keyboard to be available while using Key Mapper, you need to install "Key Mapper GUI Keyboard". It is available on the [Google Play Store](http://gui.keymapper.club), [F-Droid](https://www.f-droid.org/en/packages/io.github.sds100.keymapper.inputmethod.latin/) and [GitHub.](https://github.com/sds100/KeyMapperKeyboard/releases)

![](images/hg-settings-input-method.png) ![](images/hg-settings-input-method-light.png)

!!! failure "Important"
    If you are using a Huawei or Xiaomi branded device, a device with 2GB of RAM or less, or intend to use the app while playing a mobile game, it is especially important to follow the next step.

    Tapping the FIX button next to the battery optimisation prompt will allow you to turn off features of your device that may prevent Key Mapper's essential services from running in the background. This is usually the source of the common 'Key Mapper randomly stops working' issue.

Older devices (such as my Android 5 device pictured here) do not have this prompt in Key Mapper. However, I was still able to find the setting in my device settings app. Read more below for details.

![](images/hg-warn-battery-optimisation.png)

The screen that opens after tapping FIX may vary depending on your device. In my case, to disable optimisation I found the app(s) in the list, tapped them to open their settings, and chose 'Don't optimise' and on the second device, 'Disabled'.

!!! tip
    There is an excellent guide at [dontkillmyapp.com](http://dontkillmyapp.com) that explains how to disable battery and/or memory optimisation for your specific device. If Key Mapper randomly stops working even after you complete these steps, ask for help in [the Discord server.](http://keymapper.club)

![](images/hg-settings-battery-optimisation.png) ![](images/hg-settings-battery-optimisation-light.png)

If you want to remap volume buttons and use them when Do Not Disturb is enabled, you should grant Key Mapper permission to do so. If you have an older device, you may not have this as an option.

![](images/hg-warn-dnd.png)

Tapping FIX will allow you to grant the permission. Find Key Mapper in the list and tap it in order to choose 'Allowed'.

![](images/hg-settings-dnd.png)

Most people can ignore the final prompt. If you want to create an action to change input method, you need to grant an additional set of permissions called WRITE_SECURE_SETTINGS. For help with this [click here.](../user-guide/adb-permissions/#write_secure_settings)

![](images/hg-warn-secure-settings.png)

## Creating a key map

!!! summary
    A key map is an association between a user input (such as pushing a key or button) and a response from the device (sending information about what key was pressed and for how long).

    In Key Mapper, 'user inputs' that are recognised by the app are called 'triggers'. The responses from your device that were due to a trigger being pressed are called 'actions'.

    Key Mapper lets you assign actions to triggers that may be different from their default use. A 'volume up' trigger normally increases the volume when pressed, but this app can change that to something else.

This guide deals with 'Key event' triggers. If you want to remap fingerprint gestures, [check out this guide.](#)

### Setting the trigger(s)

To create your first key map, tap the + icon at the bottom of the Key Mapper home screen. You will see one of the two menus below.

![](images/hg-keymap-0.png) ![](images/hg-keymap-0-light.png)

In either case, the first step is to record a trigger. Tap the red 'Record trigger' button and then press the physical button that you want to change the function of. In this guide I will demonstrate with 'volume down'.

Key Mapper can also create a key map with multiple triggers. If you want to use more than one trigger for one key map, you can press the buttons in the order you will press them to execute the key map. For example, if you wanted to map 'Ctrl' + 'H' to show the device home menu, you should press 'Ctrl' and then 'H'.

Key Mapper can also remap fingerprint gestures on many devices. You can learn how to do this [here.](#)

![](images/hg-keymap-1.png) ![](images/hg-keymap-1-light.png)

### Setting the action(s)

Next, it's time to choose an action. If you have a high-resolution display, you will see the 'Add action' button at the bottom of the screen (pictured left). Otherwise, tap the 'Actions' tab at the top of the screen (pictured right).

![](images/hg-keymap-1.png) ![](images/hg-keymap-2-light.png)

Tap the 'Add action' button at the bottom of this screen. The action selection screen will open. Here you can choose from a wide variety of actions. Swipe left and right to change category, and scroll up and down the list until you find the action you want to add. Below is a table of the different kinds of actions you can choose from in each tab.

Tab | Description |
|-|-|
| App | Choose an app to open when you press your trigger. |
| App shortcut | Choose an app operation (such as sharing location, checking local traffic, sending a text message) |
| Keycode | Emulate a keyboard key press (such as an alphanumeric character or multimedia key) |
| System | Choose a system operation (such as toggling Bluetooth, opening the home menu, toggling flashlight) |
| Key | An alternative way to choose a key press action, by pressing the key that you want to map to. |
| Tap screen | Emulate a screen tap at a specific location on your screen. |
| Key event | Emulate a key press from a specifc connected device. |
| Text | Emulate typing a string. |
| Intent | See [this page.](../user-guide/actions/#intent-230) |
| Phone call | Call a telephone number. Network and carrier rates will apply. |
| Unsupported actions | A list of actions that your device won't allow you to use. Android frequently changes what users are allowed to do between versions. |

For this simple demonstration I will choose KEYCODE_E from the Keycode tab. This action will emulate pressing an E key on a keyboard.

![](images/hg-keymap-3.png) ![](images/hg-keymap-3-light.png)

!!! tip
    Key Mapper can also create a key map with multiple actions. If you want to add more actions to execute in series you can do so by tapping 'Add action' again and choosing the next action in the chain. In the next section you can specify a delay between those actions if you wish.

## Customising a key map

You may wish to customise your triggers and actions to have specific behaviours. Most people will want to do at least some customisation.

### Trigger settings

Starting with the triggers, tap the 3 dot :fontawesome-solid-ellipsis-v: menu to the right of the trigger's name to bring up the following menu.

![](images/hg-trigger-settings.png) ![](images/hg-trigger-settings-light.png) 

You can choose from four different settings. Turning on "Do not override default action" will mean that Key Mapper will not replace the normal operation of your trigger, instead it will execute the key map as well as the default operation.

One trigger can have three different click types. Choose from short press, long press and double press. [Read more.](#)

### Action settings

Next, tap the 3 stacked dots :fontawesome-solid-ellipsis-v: to the right of the action's name to bring up the following menu.

![](images/hg-action-settings.png) ![](images/hg-action-settings-light.png)

Here you can customise a lot of the operation of your key map, including timing, multipliers and debug messages.

___

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Show a toast message when performing

When this is turned on, the following notice will come up at the bottom of the screen when ever your trigger is pressed.

![](images/ui-toast.png)

You can have your device vibrate instead of an on-screen message if you want. The setting [is found here.](#)
___

The following details refer to action timing settings.

&nbsp;Delay before next action (ms)<br /> 
![](images/ui-slider-default-light-450px.png)

This option takes effect if you have multiple actions in your key map. The slider sets the amount of time between this action and the next.

___

&nbsp;Action count<br /> 
![](images/ui-slider-default-light-450px.png)

This option serves as a multiplier. If action count is equal to 1, when your key map is triggered, your action will be performed once. If it is equal to 5, the action will be performed 5 times, etc.

___

#### Repeating actions

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Repeat

When this is turned on, Key Mapper will execute your actions repeatedly. This is particularly useful when emulating a keyboard key press, where in most applications holding down the key would result in a repeating output. The default settings in Key Mapper are designed to emulate that behaviour when 'Repeat' is turned on.

___

&nbsp;Repeat every... (ms)<br /> 
![](images/ui-slider-default-light-450px.png)

This option sets the time period for your repeating action. If this is set to 200, your action will repeat every 200ms, or in terms of frequency, 5 times per second. Some people prefer to think about this setting as a repeat rate, commonly measured in 'clicks' per second or cps. To calculate the appropriate time period for this option from a desired frequency, use this equation:

```
Time period (ms) = 1000 / Frequency ('cps')
```
___

&nbsp;Delay until repeat... (ms)<br /> 
![](images/ui-slider-default-light-450px.png)

This option sets the amount of time between the first action and the second action (or the first repeat). This is part of the keyboard key behaviour simulation. If you want your repeating action to begin straight away, you can make this 0. 

___

<sup><sub>&nbsp;Stop repeating when trigger is...</sup></sub><br />
&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Released</sup></sub>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Pressed again</sup></sub>

This option allows you to choose from 2 simple behaviours. If you choose 'Released', you will need to hold down the trigger key(s) in order to have the key map execute. If you let go of the key(s), the key map will stop.

If you choose 'Pressed again', pressing your trigger key(s) once will be sufficient to start your repeating key map. To stop the key map, you will need to press the trigger key(s) a second time.

___

#### Hold down actions

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Hold down

This setting provides the ability to simulate holding down a key. With this turned on (and repeat turned off), the default behaviour is that holding down your trigger will translate to holding down the action, for as long as you hold down the trigger.

___

<sup><sub>&nbsp;Hold down until trigger is...</sup></sub><br />
&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Released</sup></sub>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Pressed again</sup></sub>

This setting allows you to choose from two behaviours. If 'Released' is selected, holding down your trigger key(s) will translate to holding down the action, for as long as you hold down the trigger key(s).

If you choose 'Pressed again', pressing your trigger key(s) once will be sufficient to start holding down the action. To stop the key map, you will need to press the trigger key(s) a second time.

___

#### Using 'Hold down' and 'Repeat' together

You can use turn on both of these settings to unlock an additional setting. A repeating hold down action behaves like a normal 'Repeat' action in that the action repeats itself with a given frequency. It also behaves like a 'Hold down' action in that each individual action will be held for a certain amount of time in each cycle.

&nbsp;Hold down duration (ms)<br /> 
![](images/ui-slider-default-light-450px.png)

This allows you to set how long the action is held down for before repeating.

## Special options

For extra key map customisation, tap the 'Constraints and more' tab at the top of the screen when editing or creating a key map.

![](images/hg-keymap-4.png) ![](images/hg-keymap-4-light.png)

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Trigger by Intent

For information about Trigger by Intent go to [this page.](../user-guide/actions/#intent-230)

___

:fontawesome-solid-check-square:{: .accent-light } &nbsp; Vibrate

&nbsp;Vibrate duration (ms)<br /> 
![](images/ui-slider-default-light-450px.png)

Tapping 'Vibrate' will cause your device to vibrate whenever your key map is triggered (and therefore behaves similarly to the [toast message](#))

## Adding constraints

You can add special constraints to your key maps in order to customise when they are allowed to run.

To add a constraint fron the 'Constraints and more' tab, tap 'Add constraint'.

You can choose from the following options:

![](images/hg-constraints-add.png) ![](images/hg-constraints-add-light.png) 

Choosing 'App in foreground' will allow you to restrict your key map to working only if your app of choice is the actively selected window, i.e. on screen and being interacted with. 'App not in foreground' will likewise restrict your key map to working only if your app of choice isn't in focus.

Choosing 'Bluetooth device is connected/disconnected' can restrict your key map to working only if a specific bluetooth device is connected/disconnected.

If you have [root permission](#), you can restrict your key maps to work only when the screen is on or off.

Choosing 'Orientation' can restrict your key map to working only when the device is set to specific screen orientation.

## Managing key maps

To save your key map and return to the home screen, tap the save :fontawesome-solid-save: icon in the bottom right of the screen.

Now your key map should already be working. To pause/unpause all of your key maps, pull down the notification tray and tap the Key Mapper notification to toggle between Paused and Running.

![](images/hg-notification.png) ![](images/hg-notification-light.png)

On the Key Mapper home screen, tap the 3 bar :fontawesome-solid-bars: menu to open the Key Mapper general settings. You will see the following options.

![](images/hg-general-settings.png) ![](images/hg-general-settings-light.png)

Here you can pause/unpause/enable/disable all of your key maps at once.<br />
You can also back up and restore key maps here. [Learn more about back up and restore.](#)

Tapping 'Show input method picker' allows for switching between a Key Mapper compatible keyboard and any other.<br />

!!! tip
    Key Mapper can also remap fingerprint gestures on many devices. You can learn how to do this [here.](#)

[Go to top.](#)