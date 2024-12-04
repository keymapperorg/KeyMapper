This page aims to explain how to remap fingerprint gestures.
For specific troubleshooting, [consult the FAQ.](../faq.md)

!!! warning
    This feature of Key Mapper does not support all devices. If you are having trouble with remapping fingerprint gestures, make sure you have read and understood the disclaimer below.

--8<-- "screenshot-version.md"

## Disclaimer

On some devices, the fingerprint sensor can be remapped by Key Mapper. There are generally 3 categories of devices with varying degrees of support for this feature:

1. Some devices have a fingerprint sensor that can detect directional gestures (up, down, left, right swipes) and your device allows third party apps such as Key Mapper to access and remap these.
2. Some devices have a fingerprint sensor that can detect directional gestures (up, down, left, right swipes) but your device does not allow Key Mapper to access them.
3. Some devices have a fingerprint sensor that is not capable of detecting directional gestures. This generally includes in-screen fingerprint sensors.

If your device falls into categories 2 or 3, you cannot remap fingerprint gestures, and you shouldn't continue with this guide. Even if your device allows for directional fingerprint gestures for system use, they may not allow third party apps such as Key Mapper to access them and therefore there is nothing the developer of Key Mapper can do to help you.

If your device supports remapping directional fingerprint gestures, this guide is for you.

## Remapping fingerprint gestures

From the Key Mapper home screen, tab the 'Fingerprint' tab.

Here you can set actions for the 4 directional gestures. Tapping any one of them will bring you to the action assignment screen for that gesture and by tapping 'Add action' at the bottom of the screen you can assign the action. [Click here for an explanation of all the actions you can choose from.](actions.md)

After choosing an action (or actions) you can press the save :material-content-save: icon in the bottom right to save the mapping.

Make sure to save :material-content-save: your fingerprint gesture map after applying these changes.

## Customising actions

If you would like to customise how the actions are performed you can tap the 3 dot menu :material-dots-vertical: to the right of an action.

On this screen you can choose from the following options:

:material-checkbox-marked:{: .accent-light } &nbsp; Hold down until swiped again

This option allows for keycode actions to be 'held', much like you could hold down a physical keyboard key. The action will continue until the same gesture is performed a second time.
___

--8<-- "action-options/delay-before-next-action.md"
___

--8<-- "action-options/action-count.md"
___

:material-checkbox-marked:{: .accent-light } &nbsp; Repeat until swiped again

When this is turned on, Key Mapper will execute your actions repeatedly. This is particularly useful when emulating a keyboard key press, where in most applications holding down the key would result in a repeating output. 

___

--8<-- "action-options/repeat-every.md"
___

--8<-- "action-options/repeat-limit.md"
___

<sup><sub>&nbsp;Stop repeating when...</sup></sub><br />
&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Swiped again</sup></sub>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:radiobox-marked:{: .accent-light } <sup><sub>Limit reached</sup></sub>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;

- **Swiped again**. Swiping the fingerprint reader once will be sufficient to start your repeating key map. To stop the key map, you will need to swipe a second time.

- **Limit reached**. (Key Mapper 2.3.0+). Your key map will stop repeating once the limit has been reached.

Tap 'Save' to close the menu, preserving changes.

## Special options

For extra customisation, tap the 'Constraints and more' tab at the top of the screen when editing or creating a gesture map.
On this screen you can choose from the following options:

--8<-- "trigger-options/vibrate.md"

---
--8<-- "trigger-options/on-screen-message.md"

## Adding constraints

--8<-- "configuring-constraints.md"