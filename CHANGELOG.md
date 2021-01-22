## [2.2.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.2.0-beta.1) 

#### 30 Dec 2020

### Added
- Remap fingerprint gestures! #378
Android 8.0+ and only on devices which support them. Even devices with the setting to swipe down for notifications might not support this! The dev can't do anything about this.

- Widget/shortcut to launch actions. #459
- Setting to show the first 5 digits of input devices so devices with the same name can be differentiated in Key Mapper lists. #470
- Show a warning at the top of the homescreen if the user hasn't disabled battery optimisation for Key Mapper. #496
- Action option to hold down until the trigger is pressed again. #479
- Action option to change the delay before the next action in the list. #476
- Orientation constraint. #505
- Key Event action option to pretend that the Key Event came from a particular device. #509
- Use duplicates of the same key in a sequence trigger. #513
- Hold down repeatedly if repeat and hold down are enabled. #500

### Changes
- No max limit for sliders (except in settings). #458

### Bug Fixes
- Save and restore state for all view models. #519
- Use View Binding in fragments properly. This should stop random crashes for some users. #518
- Hold Down action option doesn't work for long press triggers. #504
- A trigger for a specific device can still be detected if the same buttons on another device are pressed. #523
- Fix layout of the trigger fragment on some screen sizes so that some things aren't cut off. #522

## [2.1.0](https://github.com/sds100/KeyMapper/releases/tag/v2.1.0) 

#### 23 Nov 2020

This summarises the changes since 2.0.2.

### Added

- Support for Android 11.
- Backup/Restore keymaps.
- Option for keycode actions to simulate holding the key down.
- Button to show system packages as well in the app list.
- Action to create Key Event with optional modifiers.
- Action to select word at cursor.
- Action to toggle the screen on and off.
- Action to tap a coordinate on the screen. The user and the app can NOT touch the screen at the same time. This is a
limitation in Android.
- Action to double press recents to go to last app.
- Dismiss button to the notification that pauses/resumes keymaps. It will be shown again when the app is opened.
- Show a warning dialog when leaving the screen to configure a keymap without saving.
- Keymaps can have multiple of the same action. There is now a slider in the action options called "Action Count".
- Can detect the headset button when the screen is off.
- Prompt the user to reboot their device if they fail to record a trigger 2 times in a row.
- Show a toast after using the Screenshot (ROOT) action.
- Consuming the key event is optional for each key.

### Changed
- Don't hide the Repeat option if there is no trigger.

### Fixes
- Caps Lock key still caps lock when remapped.
- When making a parallel trigger, the keys don't all have the same click type.
- Dragging trigger keys by the remove button would cause a crash
- stop recording if the user leaves the Trigger fragment
- The Menu (ROOT) action was slow
- show a toast if there is an IOException when detecting buttons when the screen is off.
- Remapping modifier keys to modifier keys doesn't work as expected.
- the Screenshot (ROOT) action didn't create the Pictures and Screenshots directories. Therefore, it didn't save the screenshot.
- Hold Down action option didn't work for long-press triggers.
- Opening a keymap with a long-press parallel trigger would set it to short press.
- Crash if a modifier key trigger is not mapped to a Key Event action.
- Potential crash when showing keymaps on the homescreen.
- Attempt to fix the problem of the accessibility service being enabled but broken on some devices.
- Typo in the dialog message prompting the user to reboot.
- The dialog prompting the user to reboot would show at the wrong time.
- Switch to a new App Intro library. Hopefully it is more stable because the old library was crashing for many users.

## [2.1.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v2.1.0-beta.4) 

#### 14 Nov 2020

### Bug Fixes

- Crash on KitKat and older on the home screen.
- Consuming the key event is optional for each key.
- There was no search button under the "Other" tab when choosing an action.

## [2.1.0 Beta 3](https://github.com/sds100/KeyMapper/releases/tag/v2.1.0-beta.3) 

#### 23 Oct 2020

### Bug Fixes

- App crashed when starting the accessibility service on Android Marshmallow 6.0 and older. OOPS XD.
- The Menu action wouldn't sometimes work on rooted devices.
- Attempt to fix the problem of the accessibility service being enabled but broken on some devices.
- Typo in the dialog message prompting the user to reboot.
- The dialog prompting the user to reboot would show at the wrong time.
- Switch to a new App Intro library. Hopefully it is more stable because the old library was crashing for many users.


## [2.1.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v2.1.0-beta.2) 

#### 21 Oct 2020

### Added
- Prompt the user to reboot their device if they fail to record a trigger 2 times in a row.
- Show a toast after using the Screenshot (ROOT) action.

### Bug Fixes
- Dragging trigger keys by the remove button would cause a crash
- stop recording if the user leaves the Trigger fragment
- The Menu (ROOT) action was slow
- Entering an invalid integer into the keycode box when creating a Key Event action would cause a crash.
- show a toast if there is an IOException when detecting buttons when the screen is off.
- Remapping modifier keys to modifier keys doesn't work as expected.
- the Screenshot (ROOT) action didn't create the Pictures and Screenshots directories. Therefore, it didn't save the screenshot.
- Hold Down action option didn't work for long-press triggers.
- Opening a keymap with a long-press parallel trigger would set it to short press.
- JSON files are sometimes greyed out when picking a file to restore. All file types are now shown because Android doens't have a mimetype for JSON files.
- Crash if a modifier key trigger is not mapped to a Key Event action.
- Potential crash when showing keymaps on the homescreen.


## [2.1.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.1.0-beta.1) 

#### 29 Sept 2020

### Added
- Support for a proper keyboard. Install the Key Mapper GUI Keyboard.
- Support for Android 11.
- Backup/Restore keymaps.
- Option for keycode actions to simulate holding the key down.
- Button to show system packages as well in the app list.
- Action to create Key Event with optional modifiers.
- Action to select word at cursor.
- Action to toggle the screen on and off.
- Action to tap a coordinate on the screen. The user and the app can NOT touch the screen at the same time. This is a
limitation in Android.
- Action to double press recents to go to last app.
- Dismiss button to the notification that pauses/resumes keymaps. It will be shown again when the app is opened.
- Show a warning dialog when leaving the screen to configure a keymap without saving.
- Keymaps can have multiple of the same action. There is now a slider in the action options called "Action Count".
- Can detect the headset button when the screen is off.
- Option to not override the default behavior of the trigger.

### Changed
- Don't hide the Repeat option if there is no trigger.

### Fixes
- Caps Lock key still caps lock when remapped.
- When making a parallel trigger, the keys don't all have the same click type.

## [2.0.2](https://github.com/sds100/KeyMapper/releases/tag/v2.0.2) 

#### 31 Aug 2020

### Bug Fixes
- Fixed many crashes throughout the app. See the commit history for more detail.

### Changes
- Make the functionality to fix actions by pressing on them more discoverable. The top of the keymap on the homescreen will show "Tap actions to fix!" and the broken actions have a red tint.

## [2.0.1](https://github.com/sds100/KeyMapper/releases/tag/v2.0.1) 

#### 16 Aug 2020

### Bug Fixes
- Choosing app shortcut actions didn't work
- Remapping the Home and Recents buttons wouldn't stop them from doing their default Home/Recents actions.
- All titles for flashlight actions are the same.
- Actions didn't work on Android 11.
- Screen off triggers didn't pause.
- Screen on/off constraints showed "this action requires root" even though they weren't actions.
- Some list items weren't aligned properly if system fonts were forced to a larger size.
- Toggle Keyboard tile: Crash when switching keyboard without WRITE_SECURE_SETTINGS permission.
- A few more random crashes.

## [2.0.0](https://github.com/sds100/KeyMapper/releases/tag/v2.0.0) 

#### 22 July 2020

### Added
- Dark mode! 🕶
- A keymap can have multiple actions.
- Triggers
  - 2 modes. The keys can all be pressed at the same time or one after another in a sequence.
  - Keys can be limited to a specific external device, any device or the device the app is installed on.
  - Double press support.
- Constraints. Keymaps can be restricted to only work in certain situations. Constraints can be mixed in OR mode or AND mode.
  - App in foreground
  - App not in foreground
  - Bluetooth device connected
  - Bluetooth device not connected
   - Screen on/off (ROOT only).

- Actions
  - Toggle/enable/disable a Do Not Disturb mode (Android 6.0+).
  - Toggle/enable/disable airplane mode (ROOT only).
  - Switch between vibrate and ring.
  - Launch the device assistant rather than the voice assistant.
  - Take screenshots on rooted devices older than Pie.
  - Can now have unique repeat options and any action is allowed to be repeated now.
  - Show the keycode number when picking a Keycode action.

- Renamed "Repeat Delay" to "Repeat Rate".
- Renamed "Hold Down Delay" to "Repeat Delay"
- Modifier keys now affect Key and Keycode actions.
- Option to vibrate twice for long press actions. Once when initially pressing the keys and again when the action is performed.
- Option for keymaps with volume key triggers to be detected when the screen is off (ROOT only).
- Option to stop repeating an action when the trigger is pressed again.
- Button in the homescreen menu to resume/pause keymaps and enable the accessibility service.
- Setting to hide the alerts at the top of the homescreen.
- Notification to toggle the Key Mapper keyboard.
- Quick Settings to toggle the Key Mapper keyboard and pause/resume keymaps.
- Duplicate keymaps.
- Screen to configure keymaps is more optimised for very large screens.
- Preference to switch to and from the Key Mapper keyboard when pausing/resuming keymaps.
- The option to show the "performing action" toast has been moved to a toggle in each keymap.
- The long press delay, double press timeout, sequence trigger timeout, action repeat delay, hold-down delay until actions are repeated and vibrate delay can be changed per keymap.
- Keymaps which have modifier key actions now affect other keymaps and keys which aren't mapped.
- Link to the Discord server in About.

### Bug Fixes
- App Shortcut actions now work properly!
- The code base has completely changed so some bugs in 1.1.7 could have been fixed.

### Changes
- Keymaps can only have one trigger. Any keymaps with multiple triggers will be split up into multiple keymaps.

### Removed
- The in-app logger. Send Android bug reports instead.
- Showing the Input Method picker on Android 10 and newer because Android dropped support.

## [2.0.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v2.0.0-beta.4) 

#### 17 July 2020

Only bug fixes.

### Changes
- Renamed "Repeat Delay" to "Repeat Rate".
- Renamed "Hold Down Delay" to "Repeat Delay"

### Bug Fixes
- Crash when leaving app the menu to tweak an action showing.
- Double press triggers aren't detected.

## [2.0.0 Beta 3](https://github.com/sds100/KeyMapper/releases/tag/v2.0.0-beta.3) 

#### 02 July 2020

Significantly improved the input latency.

### Added
- Actions can now have unique repeat options and any action is allowed to be repeated now.
- Screen on/off constraints (ROOT only).
- Option for keymaps with volume key triggers to be detected when the screen is off (ROOT only).
- Option to stop repeating an action when the trigger is pressed again.
- Button in the homescreen menu to resume/pause keymaps and enable the accessibility service.
- Setting to hide the alerts at the top of the homescreen.
- Action to take screenshots on rooted devices older than Pie.

### Bug Fixes
- Triggers with the Recents and Home button would sometimes open Recents and go Home.
- Increase the screen width threshold to put all the cards in one tab to 1000dp.
- Don't crash when sometimes changing a slider.
- Lower the max repeat delay to 1000ms to make it easier to pick tiny values.
- Increase the min repeat delay to 5ms because 0ms caused crashes.

### Changes
- Persist whether keymaps are paused.
- The "Switch Keyboard" action now works when the app has WRITE_SECURE_SETTINGS permission rather than just rooted devices.

### Removed
- Setting to show a toast message when an action fails. Removing this made improving the input latency much easier.

## [2.0.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v2.0.0-beta.2) 

#### 16 Jun 2020

### Added

- Action to toggle/enable/disable a Do Not Disturb mode (Android 6.0+).
- Action to toggle/enable/disable airplane mode (ROOT only).
- Action to switch between vibrate and ring.
- Action to launch the device assistant rather than the voice assistant.
- Notification to toggle the Key Mapper keyboard.
- Quick Settings to toggle the Key Mapper keyboard and pause/resume keymaps.
- Keymap option to vibrate twice for long press actions. Once when initially pressing the keys and again when the action is performed.
- Duplicate keymaps.
- Screen to configure keymaps is more optimised for very large screens.
- Preference to switch to and from the Key Mapper keyboard when pausing/resuming keymaps.

### Bug Fixes

- Stop repeating actions when another key is pressed.
- Parallel triggers would be forced to short press when its keymap was edited.
- Don't show the same dialog multiple times when configuring keymaps.
- Automatically expand Bottom Sheet menus.
- Don't consume keyevents when actions for parallel triggers fail.
- Short press and long press triggers don't cross over.
- Short press and double press triggers don't cross over.
- Wifi actions didn't work on Android Pie. Android doesn't allow apps to control WiFi anymore so these actions have been restricted to rooted devices on Android 9.0+ .
- Crash when sometimes changing keymap options with a slider.
- Sequence trigger timeout option was shown for a single key double press trigger.
- Crash when launching the app for the first time in landscape.

## [2.0.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.0.0-beta.1) 🎉

#### 08 Jun 2020
### Added

- Dark mode! 🕶

- A keymap can have multiple actions.
- Triggers
  - 2 modes. The keys can all be pressed at the same time or one after another in a sequence.
  - Keys can be limited to a specific external device, any device or the device the app is installed on.
  - Double press support.
- Constraints. Keymaps can be restricted to only work in certain situations. Constraints can be mixed in OR mode or AND mode.
  - App in foreground
  - App not in foreground
  - Bluetooth device connected
  - Bluetooth device not connected
- The option to show the "performing action" toast has been moved to a toggle in each keymap.
- The long press delay, double press timeout, sequence trigger timeout, action repeat delay, hold-down delay until actions are repeated and vibrate delay can be changed per keymap.
- Modifier keys now affect Key and Keycode actions.
- Keymaps which have modifier key actions now affect other keymaps and keys which aren't mapped.
- Show the keycode number when picking a Keycode action.
- Link to the Discord server in About.

### Bug Fixes

- App Shortcut actions now work properly!

- The code base has completely changed so some bugs in 1.1.7 could have been fixed.

### Changes

- Keymaps can only have one trigger. Any keymaps with multiple triggers will be split up into multiple keymaps.

### Removed

- The in-app logger. Send Android bug reports instead.
- Showing the Input Method picker on Android 10 and newer because Android dropped support.

## [1.1.7](https://github.com/sds100/KeyMapper/releases/tag/v1.1.7)
#### 07 Jan 2020

### Bug Fixes
- KEYCODE_BACK appeared twice in the keycode list.
- crashed when the battery optimisation settings couldn't be found.
- some trigger keys have no name.
- unable to uncheck the "show volume dialog" flag.
- on some devices (e.g Oxygen OS 10),  the volume buttons up keyevents need to be consumed to stop them from changing the volume when performing an action.
- couldn't necessarily press the back button to get back to Key Mapper when opening the accessibility settings.

### Added
- support for Jelly Bean 4.2 and 4.3.
- setting to show the toast message when an action fails to perform.
- action to open the device settings.
- action to open a URL.
- action to switch the input method (ROOT only)
- action to show the power dialog (Android 5.0+)
- action to lock the device without root (only Android 9.0+)
- action to toggle split screen (Android 7.0+)

### Changes
- Removed Firebase.

## [1.1.6](https://github.com/sds100/KeyMapper/releases/tag/v1.1.6)
#### 03 Nov 2019

F-Droid can now build.

## [1.1.5](https://github.com/sds100/KeyMapper/releases/tag/v1.1.5)
#### 03 Nov 2019
This is the first release to be released on F-Droid.

### Removed
- Firebase library.

### Bug Fix
- KEYCODE_BACK appeared twice in the keycode action list. #247 

## [1.1.4](https://github.com/sds100/KeyMapper/releases/tag/v1.1.4)
#### 22 Aug 2019
### Bug Fixes
- App crashed when opening the choose action activity on KitKat devices.

## [1.1.3](https://github.com/sds100/KeyMapper/releases/tag/v1.1.3)
#### 20 Aug 2019
### Bug Fixes
- App crashed after updating.

## [1.1.2](https://github.com/sds100/KeyMapper/releases/tag/v1.1.2)
#### 19 Aug 2019
### Bug Fixes
- Make all slides in the intro activity scrollable so the content can be displayed on smaller devices
- Remapping the recents button would still open recents
- Crash when the app was rotated in the "choose action" activity
- Triggers are ignored when another trigger is being detected.

### Added
- Action to show the keyboard picker
- Guide the user to grant WRITE_SECURE_SETTINGS for the app so features previously restricted to rooted devices can be used on all devices.
- Slide to enable Do Not Disturb in the intro activity.

### Changed
- Rename strings for the keyboard picker notification
- Use unique keyboard names for CI and debug builds.

## [1.1.1](https://github.com/sds100/KeyMapper/releases/tag/v1.1.1)
#### 27 July 2019
Exact same as 1.1.0 besides the version code and name. I messed up the versioning on Google play so had to increment the version code.

## [1.1.0](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0)
#### 27 July 2019

The initial release for Key Mapper.

Changes from 1.1.0 Beta 8:
- Moved the dialog to opt in to analytics to a slide in the intro activity.

## [1.1.0 Beta 8](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.8)
#### 20 July 2019
### Bug Fixes
- Changes to a keymap wouldn't persist after a configuration change (e.g rotation)
- The keyboard service status layout at the top of the homescreen wouldn't update.
- fix minor inconsistencies in the app icons

### Changed
- The action to simulate the menu button no longer requires root
- Use a countdown timer when recording a trigger
- Support Android Q

### Added
- Show an error on the homescreen and if an action needs the Key Mapper keyboard to be enabled.
- Show an error when trying to use an action which requires the Key Mapper keyboard and it is disabled.
- Action to move the cursor to the end of a file
- Actions to toggle, show and hide the keyboard
- Button to change the keyboard in the homescreen menu
- About: link to the Telegram channel
- Show a prompt to enable the accessibility service when the app is first opened.
- Explain why the "record trigger" button is greyed out
- labels for the KEYCODE_BUTTON_START and KEYCODE_BUTTON_SELECT keycodes
- An introduction activity the first time the app is opened
- Logger: log when recording a trigger has started and stopped
- Show a dialog the first time the Key Mapper keyboard is chosen explaining why another keyboard can't be used.
- ChooseActionActivity: A tab to which lists all the actions which aren't supported and why.
- Show a "requires root" message for actions which need it

## [1.1.0 Beta 7](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.7)
#### 27 May 2019
### Bug Fixes
- App would crash when trying to read a system setting which doesn't exist
- App would crash if couldn't find the Do Not Disturb settings page
- Logger: send icon was grey but should be white 

### Added
- The status card at the top of the homescreen can now be expanded and collapsed
- Show a toast message when a foreseen error is encountered
- Logger: log whenever the accessibility service is started/stopped

## [1.1.0 Beta 6](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.6)
#### 19 May 2019
### Bug Fixes
 - Don't show NFC actions on devices without NFC
 - Couldn't change volume when short pressing a volume button remapped to a long press action

## [1.1.0 Beta 5](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.5)
#### 19 May 2019
 - Updated libraries
### Added
 - Action to enable, disable and toggle NFC.
 - Action to switch between portrait and landscape mode.
 - Action to cycle through and change the ringer mode.
 - Action to fast forward and rewind.
 - Option to log events and send them to the developer so it is easier to debug issues with the app.
 - Button to Help page on homescreen.
 - Translation instuctions to the About activity.

### Changes
 - Add the trigger after the 5 seconds rather than having to press the button so the app can work with devices which only have remotes as input.
 - Cleanup Settings strings.
 - Use slightly darker homescreen background.
 - Don't show the "Key mapper is performing an action" toast message by default.
 - Allow the volume to be changed while in Do Not Disturb mode.
 - Minimum vibration duration is 1ms rather than 100ms

### Bug Fixes
- The landscape mode action wouldn't work.
- Would potentially crash when trying to open the write-settings permission page.
- Don't show a toast message when enabling/disabling the device admin.
- Would crash when selecting a shortcut without the correct permissions.
- Device would go to the homescreen when using a trigger with the home button in it.

## [1.1.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.4)
#### 10 Apr 2019
### Added
- Option to choose which flash to use for flashlight actions
- Optimised the New and Edit Keymap activities for various screen sizes
- Slightly optimised the homescreen for wide screens

### Bug Fixes
- Could potentially crash when trying to switch to the Key Mapper input method
- Could potentially crash when removing a trigger from the list
- Would crash if it couldn't find the input method settings page
- Would crash when trying to change a specific volume stream while the device is in a Do Not Disturb state
- Would crash when using an app shortcut without the correct permissions.

## [1.1.0 Beta 3](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.3)
#### 4 Apr 2019
- Reduced the repeat delay to 5ms
- Force expand the menu on the homescreen
- Made the cards on the homescreen slightly more compact

### Added
- Flag to vibrate and an option to force vibrate for all actions
- Action which just consumes the keyevent and does nothing
- Action to lock the device (ROOT only for now) and an option to lock the device securely (without root).

### Bug fixes
- The bottom app bar on the homescreen would overlap the list items
- The app would potentially crash when trying to perform a flashlight action whilst the camera is in use in another app.
- Short press actions with the same trigger as a long press action would be performed with the long press action
- A keymap would still have the "Show volume dialog" flag if the action is changed to a non volume related action
- The app would crash if trying to show the menu on the homescreen if it is already showing.
- The accessibility service status on the homescreen wouldn't change when the service is started/stopped.

## [1.1.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.2)
#### 31 Mar 2019
- Won't immediately crash on KitKat anymore! :)

### Bug fixes
- Persistent IME notification wouldn't automatically show when it is enabled.
- App would crash if it couldn't find the device's accessibility settings page.

## [1.1.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v1.1.0-beta.1)
#### 27 Mar 2019
### Added
- Setting to change the long-press delay.
- Persistent notification which can pause/resume your remaps. It can also open the accessibility settings on the device to enable/disable the service. Rooted devices can start/stop the accessibility service without going into settings and just tap the notification.
- Use Material Design 2 for homescreen.

### Bug fixes
- Persistent notifications wouldn't show on boot
- The app would crash if using the "open google assistant" action if the Google app wasn't installed.
- Prevent the accessibility service from stopping if there is a fatal exception and show a toast when it happens.


## [1.0.0 Beta 6](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.6)
#### 22 Mar 2019
- Changed developer email.
- Added link to the XDA Thread in the About activity.

## [1.0.0 Beta 5](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.5)
#### 22 Mar 2019
- Updated build-tools to 28.0.4
- Updated Room library to 2.1.0-alpha05
- Updated Firebase core library to 16.0.8

#### Bug fixes
- App would crash when using brightness actions because it needed write system settings permission.

## [1.0.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.4)
#### 9 Mar 2019
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
#### 2 Mar 2019
- added option to email developer in the About activity.
- created privacy policy and ability to opt in/out of Firebase analytics

## [1.0.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v1.0.0-beta.1)
#### 2 Mar 2019
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
