This page aims to give users an introduction to the Key Mapper interface and a demonstration on how to perform typical tasks with the app.
For specific troubleshooting, [consult the FAQ.](faq.md)

![Visitors](https://visitor-badge.glitch.me/badge?page_id=qsg)

--8<-- "screenshot-version.md"

## First time setup

When you open Key Mapper, you will be presented with this screen.
This screen is referred to as the Key Mapper home screen.

![](images/hg-home-empty.png)

### Set up the accessibility service
Tapping the FIX button next to the accessibility service prompt will allow you to grant Key Mapper permission to run its accessibility service. You must do this for the app to work at all.

![](images/hg-warn-service.png)

You can then tap Key Mapper in your list of available services. There may be many others on this screen and your interface may be slightly different.

!!! attention
    If nothing happens when you tap FIX, or if you can't enable the service, see [this page](user-guide/adb-permissions.md#enabling-the-accessibility-service) or ask for help in the [Discord server.](http://keymapper.club)

![](images/hg-settings-services.png)

### Disable app-killing

!!! failure "Important"
    If you are using a Huawei or Xiaomi branded device, a device with 2GB of RAM or less, or intend to use the app while playing a mobile game, it is especially important to follow the next step.

    Tapping the FIX button next to the battery optimisation prompt will allow you to turn off features of your device that may prevent Key Mapper's essential services from running in the background. This is usually the source of the common 'Key Mapper randomly stops working' issue.

Devices older than Android 6.0 do not have this prompt in Key Mapper. However, I was still able to find the setting in my device settings app. Read more below for details.

![](images/hg-warn-battery-optimisation.png)

The screen that opens after tapping FIX may vary depending on your device. In my case, to disable optimisation I found the app(s) in the list, tapped them to open their settings, and chose 'Don't optimise' and on the second device, 'Disabled'.

!!! tip
    There is an excellent guide at [dontkillmyapp.com](http://dontkillmyapp.com) that explains how to disable battery and/or memory optimisation for your specific device. If Key Mapper randomly stops working even after you complete these steps, ask for help in [the Discord server.](http://keymapper.club)

## Creating a key map

!!! summary
    A key map is an association between a user input (such as pushing a key or button) and a response from the device (sending information about what key was pressed and for how long).

    In Key Mapper, 'user inputs' that are recognised by the app are called 'triggers'. The responses from your device that were due to a trigger being pressed are called 'actions'.
    
    Key Mapper lets you assign actions to triggers that may be different from their default use. A 'volume up' trigger normally increases the volume when pressed, but this app can change that to something else.

This guide deals with 'Key event' triggers. If you want to remap fingerprint gestures, [check out this guide.](user-guide/fingerprint-gestures.md)

### Setting the trigger(s)

To create your first key map, tap the + icon at the bottom of the Key Mapper home screen. You will see the screen below. On larger screens this will look slightly different because multiple tabs will be merged into one tab.

![](images/config-keymap-empty.png)

In either case, the first step is to record a trigger. Tap the red 'Record trigger' button and then press the physical button that you want to change the function of. In this guide I will demonstrate with 'volume down'.

Key Mapper can also create a key map with multiple triggers. If you want to use more than one trigger for one key map, you can press the buttons in the order you will press them to execute the key map. For example, if you wanted to map 'Ctrl' + 'H' to show the device home menu you should press 'Ctrl' and then 'H'.

Key Mapper can also remap fingerprint gestures on many devices. You can learn how to do this [here.](user-guide/fingerprint-gestures.md)

You should see this screen after pressing volume down.

![](images/config-keymap-trigger-dnd-error.png)

### Setup the ability to properly remap volume buttons

You only need to do this on Android 6.0+. If you want to remap volume buttons and use them when Do Not Disturb is enabled, you should grant Key Mapper permission to do so. Key Mapper will show the error in the picture above if you need to do this.

Tapping FIX will allow you to grant the permission. Find Key Mapper in the list and tap it in order to choose 'Allowed'.

![](images/hg-settings-dnd.png)

### Setting the action(s)

Next, it's time to choose an action. If you have a large screen, you will see the 'Add action' button at the bottom of the screen. Otherwise, tap the 'Actions' tab at the top of the screen and then you will see the 'Add action' button.

![](images/config-keymap-actions-tab.png)

Tap the 'Add action' button at the bottom of this screen. The action selection screen will open. Here you can choose from a long list of actions. There is also a list of all the actions [here](user-guide/actions.md). 

For this simple demonstration I will choose KEYCODE_0 from the Keycode tab. This action will emulate pressing the 0 key on a keyboard. Key Mapper will let you know if you need to do any extra steps so that an action can work. In the picture there is an error message in the action telling you to 'enable' a Key Mapper keyboard. Tapping the action will take you to the correct place so that you can fix the error. I will walk through how to set up a key code action since these are the most used.

![](images/config-keymap-keycode-action-enable-ime-error.png)

!!! tip
    Key Mapper can also create a key map with multiple actions. If you want to add more actions to execute in series you can do so by tapping 'Add action' again and choosing the next action in the chain. You can drag and drop the actions into whatever order you like. In the next section you can specify a delay between those actions if you wish.

### Fixing the action(s)

1. Tap the action to fix the error.

2. If you are creating a key code, key event or text action then you **MUST** read [this](user-guide/key-mapper-keyboard.md) guide

3. In this case tapping the action will take you to the 'Language & Input' settings on your device.  On a stock Android 11 Pixel 2 this is what it looks like. You will see a list of all the installed input methods on your device.

    ![](images/settings-enable-ime.png)

4. It is important to have read the guide in step 2 to understand what is happening from now on. On my device I installed the Key Mapper GUI Keyboard so I am going to turn this one on. Press back to go back to Key Mapper.

5. There is now a 2nd thing that needs to be fixed as you can see in the picture.

    ![](images/config-keymap-keycode-action-choose-ime-error.png)

6. Tap the action again and the input method picker will show up as shown in the picture. On Android 11+ Key Mapper can fix this error for you without showing the input method picker if you have enabled the accessibility service.

    ![](images/config-keymap-choose-ime.png)

7. Since I enabled the Key Mapper GUI Keyboard I will use that input method with Key Mapper.

8. If you followed these steps correctly then Key Mapper will stop showing any error messages underneath the action as shown. This action will now work.

    ![](images/config-keymap-actions-no-error.png)

## Customising a key map

You may wish to customise your triggers and actions to have specific behaviours. Most people will want to do at least some customisation.

### Trigger settings

Starting with the triggers, tap the 3 dot :material-dots-vertical: menu to the right of the trigger's name to bring up the following menu.

![](images/hg-trigger-settings.png)

You can choose from four different settings. Turning on "Do not override default action" will mean that Key Mapper will not replace the normal operation of your trigger, instead it will execute the key map as well as the default operation.

One trigger can have three different click types. Choose from short press, long press and double press. [Read more.](../user-guide/keymaps.md#key-click-type)

### Customising actions

Next, tap the 3 stacked dots :material-dots-vertical: to the right of the action's name to bring up the following menu.

![](images/hg-action-settings.png)

Here you can customise a lot of the operation of your key map, including timing and multipliers.

___

The following details refer to action timing settings.

--8<-- "action-options/delay-before-next-action.md"
___

--8<-- "action-options/action-count.md"
___

#### Repeating actions

--8<-- "action-options/repeat.md"
___

--8<-- "action-options/repeat-every.md"
___

--8<-- "action-options/delay-until-repeat.md"
___

--8<-- "action-options/repeat-behaviour.md"

#### Hold down actions

--8<-- "action-options/hold-down.md"
___

--8<-- "action-options/hold-down-behaviour.md"

#### Using 'Hold down' and 'Repeat' together

--8<-- "action-options/using-hold-down-and-repeat-together.md"

## Special options

You can see explanations of more options [here](../user-guide/keymaps/#special-options).

## Adding constraints

--8<-- "configuring-constraints.md"

## Managing key maps

To save your key map and return to the home screen, tap the save :material-content-save: icon in the bottom right of the screen.

Now your key map should already be working. To pause/unpause all of your key maps, pull down the notification tray and tap the Key Mapper notification to toggle between Paused and Running.

![](images/hg-notification.png) ![](images/hg-notification-light.png)

On the Key Mapper home screen, tap the 3 bar :fontawesome-solid-bars: menu to open the Key Mapper general settings. You will see the following options.

![](images/hg-general-settings.png) ![](images/hg-general-settings-light.png)

Here you can pause/unpause/enable/disable all of your key maps at once.<br />
You can also back up and restore key maps here. [Learn more about back up and restore.](user-guide/backup-restore.md)

Tapping 'Show input method picker' allows for switching between a Key Mapper compatible keyboard and any other.<br />

!!! tip
    Key Mapper can also remap fingerprint gestures on many devices. You can learn how to do this [here.](user-guide/fingerprint-gestures.md)