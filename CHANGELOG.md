# Changelog 

All notable changes to this project will be documented in this file. This project adheres to semantic versioning.
 
## [Unreleased](https://github.com/sds100/KeyMapper/tree/develop)

## [1.0.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.4)
- Added more labels for keys.
- Added a link to the app in the device's Accessibility settings.
- Updated the Gradle version to 3.3.2
- When the long-press flag is chosen, show a warning saying it will only work properly for volume and navigation buttons.
- Enable the show-volume-ui flag by default.

#### Bug fixes
- App would crash when choosing flags for a keymap without an action.
- Buttons being repeatedly pressed.
- Enabling the long-press flag would stop the button from working when it is pressed without a long press. 

## Accidentally skipped Beta 3 release. Oops.

## [1.0.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.2)
- added option to email developer in the About activity.
- created privacy policy and ability to opt in/out of Firebase analytics


## [1.0.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.1)
- Initial release!
- Option to automatically change the input method and/or show the input method picker when a chosen Bluetooth device is connected and switch back to the old one when disconnected
- Option to show a notification, which when clicked on, will show the input method picker. Android 8.1+ needs root.
- Option to show a toast message whenever an action is performed.
- A Help activity
- An About activity
- No limit on the amount of triggers for a keymap and how many keys can be used to create a trigger.
- Optional flags for each keymap so it can only be triggered on a long press and whether to show the volume dialog for volume related actions.
- Ability to enable/disable specific/all keymaps.

   #### Added these actions
   - Open App
   - Open App shortcut
   - A keycode
   - A key
   - A block of text
   
   - Go back
   - Go home
   - Open recents
   - Open menu
   
   - Expand notification drawer
   - Expand quick settings
   - Collapse status bar
   
   - Toggle/enable/disable WiFi  
   - Toggle/enable/disable Bluetooth
   - Toggle/enable/disable mobile data
   
   - Toggle Play/pause media
   - Pause,play media
   - Next/previous track
   
   - Volume up/down
   - Increase/decrease a specific volume stream
   - Show the volume dialog
   - Mute/unmute/toggle mute
   
   - Toggle/enable/disable auto-rotate
   - Force portrait/landscape mode
   
   - Toggle/enable/disable auto-brightness
   - Increase/decrease brightness
   
   - Toggle/enable/disable flashlight
   
   - Screenshot
   - Open Google Assistant
   - Open camera
   
