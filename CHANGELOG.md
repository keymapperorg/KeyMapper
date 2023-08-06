## [2.4.5](https://github.com/sds100/KeyMapper/releases/tag/v2.4.5)

#### 5 August 2023

### Changed
- [#1120](https://github.com/keymapperorg/KeyMapper/issues/1120) Do not change the UIDs of key maps when importing them.

## [2.4.4](https://github.com/sds100/KeyMapper/releases/tag/v2.4.4)

#### 21 July 2022

### Bug fixes

- [#1073](https://github.com/keymapperorg/KeyMapper/issues/1073) The toggle key maps quick settings tile now looks
  enabled when the key maps are resumed.
- [#1062](https://github.com/keymapperorg/KeyMapper/issues/1062) The select word at cursor action would select the whole
  line if the cursor is at the end of the line.
- [#999](https://github.com/keymapperorg/KeyMapper/issues/999) Remove delay when launching app shortcut actions when at
  the launcher.

### Added

- [#1054](https://github.com/keymapperorg/KeyMapper/issues/1054) Make it clearer why key map launcher shortcut can't be
  created automatically.
- [#1068](https://github.com/keymapperorg/KeyMapper/issues/1068) Make the confirmation dialog when leaving without
  saving clearer.

## [2.4.3](https://github.com/sds100/KeyMapper/releases/tag/v2.4.3)

#### 21 June 2022

### Bug fixes

- [#1052](https://github.com/keymapperorg/KeyMapper/issues/1052) Crash when disabling accessibility service through the
  Key Mapper notification on Android Lollipop and Marshmallow.
- [#1049](https://github.com/keymapperorg/KeyMapper/issues/1049) Enabling the accessibility service automatically with
  WRITE_SECURE_SETTINGS wouldn't work if no other accessibility features were enabled.
- [#1044](https://github.com/keymapperorg/KeyMapper/issues/1044) On some devices there was a random crash in the
  notification listener service.
- [#999](https://github.com/keymapperorg/KeyMapper/issues/999) Action to launch app has a 5-10 second delay when you're
  on your device's home screen.
- [#1047](https://github.com/keymapperorg/KeyMapper/issues/1047) App NOT playing media constraint would be saved as
  wrong constraint.
- [#1043](https://github.com/keymapperorg/KeyMapper/issues/1043) Inputting key events with Shizuku didn't actually work
  on release builds. OOPS. 🤦‍

### Added

- [#1025](https://github.com/keymapperorg/KeyMapper/issues/1025) Support for Android 12L.
- [#1016](https://github.com/keymapperorg/KeyMapper/issues/1016) Ability to never show the denied Do Not Disturb
  permission errors if the device does not support those settings.
- [#1042](https://github.com/keymapperorg/KeyMapper/issues/1042) Put date in the timestamp in the log.

## [2.4.2](https://github.com/sds100/KeyMapper/releases/tag/v2.4.2)

#### 27 May 2022

### Bug fixes

- [#1017](https://github.com/keymapperorg/KeyMapper/issues/1017) The app would go in an infinite loop saying "Using root
  to grant WRITE_SECURE_SETTINGS permission" on screen and then eventually crashing.

## [2.4.1](https://github.com/sds100/KeyMapper/releases/tag/v2.4.1)

#### 24 May 2022

### Added

- [#989](https://github.com/keymapperorg/KeyMapper/issues/989) Constraint for charging/discharging.

### Bug fixes

- Crash if trying to grant permission with Shizuku but Key Mapper doesn't have Shizuku permission.
- [#1009](https://github.com/keymapperorg/KeyMapper/issues/1009) Crash when trying to edit actions.
- [#1001](https://github.com/keymapperorg/KeyMapper/issues/1001) Crash when granting permission with Shizuku on Android
  12+.

## [2.4.0](https://github.com/sds100/KeyMapper/releases/tag/v2.4.0)

#### 08 May 2022

See beta releases for bug fixes.

### Added

#### Important

- [#748](https://github.com/keymapperorg/KeyMapper/issues/748) Android 12 and Material You support 🎨!
- [#746](https://github.com/keymapperorg/KeyMapper/issues/746) Shizuku support for some features! You can use this
  instead of using a Key Mapper keyboard!

#### Actions

- [#603](https://github.com/keymapperorg/KeyMapper/issues/603) You can now edit actions! You don't have to delete an
  action and completely reconfigure it.
- [#851](https://github.com/keymapperorg/KeyMapper/issues/850) Action to answer/end a phone call.
- [#704](https://github.com/keymapperorg/KeyMapper/issues/704) Action to dismiss notifications.

#### Constraints

- [#851](https://github.com/keymapperorg/KeyMapper/issues/851) Constraints for when the device is ringing and in a phone
  call.
- [#811](https://github.com/keymapperorg/KeyMapper/issues/811) Constraint for when the device is locked.
- [#776](https://github.com/keymapperorg/KeyMapper/issues/776) Constraint for when an input method is chosen.
- [#598](https://github.com/keymapperorg/KeyMapper/issues/598) Constraint for any app (not) playing or a specific app
  not playing media.
- [#702](https://github.com/keymapperorg/KeyMapper/issues/702) WiFi on/off/connected/disconnected constraints.
- [#722](https://github.com/keymapperorg/KeyMapper/issues/722) Flashlight on/off constraint.

#### Other

- [#911](https://github.com/keymapperorg/KeyMapper/issues/911) Detect camera button when screen is off.
- [#780](https://github.com/keymapperorg/KeyMapper/issues/780) If the accessibility settings can't be found prompt the
  user to follow an online guide to do it with ADB.
- [#773](https://github.com/keymapperorg/KeyMapper/issues/773) Prompt for a message when the user reports a bug.
- [#686](https://github.com/keymapperorg/KeyMapper/issues/686) Setting to switch to a different input method on input
  focus.
- [#715](https://github.com/keymapperorg/KeyMapper/issues/715) Show a share button after a successful backup.
- [#716](https://github.com/keymapperorg/KeyMapper/issues/716) Support for a Key Mapper compatible version of Hacker's
  Keyboard. Releases can be found here: https://github.com/keymapperorg/KeyMapperHackersKeyboard/releases
- [#955](https://github.com/keymapperorg/KeyMapper/issues/955) You can now detect the Menu and Search button when the
  screen is off. On some devices the Bixby button is detected by Key Mapper as the Menu button.
- [#928](https://github.com/keymapperorg/KeyMapper/issues/928) You can now call Termux RUN_COMMAND intents because the
  necessary permission has been added to Key Mapper.

## [2.4.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v2.4.0-beta.02)

#### 29 April 2022

### Bug fixes

- [#946](https://github.com/keymapperorg/KeyMapper/issues/946) Choosing a Bluetooth device disconnected constraint would
  save it as a connected constraint.
- [#981](https://github.com/keymapperorg/KeyMapper/issues/981) Trying to backup/restore would crash the app if no
  file-picker was installed.
- [#965](https://github.com/keymapperorg/KeyMapper/issues/965) The app would crash if you had disabled key maps.
- [#957](https://github.com/keymapperorg/KeyMapper/issues/957) The dialog to choose flags for an Intent action wouldn't
  show the flags that had already been picked.

### Added

- [#955](https://github.com/keymapperorg/KeyMapper/issues/955) You can now detect the Menu and Search button when the
  screen is off. On some devices the Bixby button is detected by Key Mapper as the Menu button.
- [#928](https://github.com/keymapperorg/KeyMapper/issues/928) You can now call Termux RUN_COMMAND intents because the
  necessary permission has been added to Key Mapper.

## [2.4.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.4.0-beta.01)

#### 19 March 2022

### Changes

- [#815](https://github.com/keymapperorg/KeyMapper/issues/815) Always show the button to pick a package when configuring
  an intent action.
- [#749](https://github.com/keymapperorg/KeyMapper/issues/749) Remove do not disturb app intro slide.
- [#750](https://github.com/keymapperorg/KeyMapper/issues/750) Redesign the About screen.
- [#747](https://github.com/keymapperorg/KeyMapper/issues/747) Reorganise the Settings screen so it is less cluttered.

### Added

#### Important

- [#748](https://github.com/keymapperorg/KeyMapper/issues/748) Android 12 and Material You support 🎨!
- [#746](https://github.com/keymapperorg/KeyMapper/issues/746) Shizuku support for some features! You can use this
  instead of using a Key Mapper keyboard!

#### Actions

- [#603](https://github.com/keymapperorg/KeyMapper/issues/603) You can now edit actions! You don't have to delete an
  action and completely reconfigure it.
- [#851](https://github.com/keymapperorg/KeyMapper/issues/850) Action to answer/end a phone call.
- [#704](https://github.com/keymapperorg/KeyMapper/issues/704) Action to dismiss notifications.

#### Constraints

- [#851](https://github.com/keymapperorg/KeyMapper/issues/851) Constraints for when the device is ringing and in a phone
  call.
- [#811](https://github.com/keymapperorg/KeyMapper/issues/811) Constraint for when the device is locked.
- [#776](https://github.com/keymapperorg/KeyMapper/issues/776) Constraint for when an input method is chosen.
- [#598](https://github.com/keymapperorg/KeyMapper/issues/598) Constraint for any app (not) playing or a specific app
  not playing media.
- [#702](https://github.com/keymapperorg/KeyMapper/issues/702) WiFi on/off/connected/disconnected constraints.
- [#722](https://github.com/keymapperorg/KeyMapper/issues/722) Flashlight on/off constraint.

#### Other

- [#911](https://github.com/keymapperorg/KeyMapper/issues/911) Detect camera button when screen is off.
- [#780](https://github.com/keymapperorg/KeyMapper/issues/780) If the accessibility settings can't be found prompt the
  user to follow an online guide to do it with ADB.
- [#773](https://github.com/keymapperorg/KeyMapper/issues/773) Prompt for a message when the user reports a bug.
- [#686](https://github.com/keymapperorg/KeyMapper/issues/686) Setting to switch to a different input method on input
  focus.
- [#715](https://github.com/keymapperorg/KeyMapper/issues/715) Show a share button after a successful backup.
- [#716](https://github.com/keymapperorg/KeyMapper/issues/716) Support for a Key Mapper compatible version of Hacker's
  Keyboard. Releases can be found here: https://github.com/keymapperorg/KeyMapperHackersKeyboard/releases

### Bug Fixes

- [#794](https://github.com/keymapperorg/KeyMapper/issues/794) Only list apps that can be launched when creating an open
  app action.
- [#823](https://github.com/keymapperorg/KeyMapper/issues/823) Can't choose an app when creating media action.
- [#756](https://github.com/keymapperorg/KeyMapper/issues/756) On slighter smaller screens show split layout when
  configuring a mapping.
- [#739](https://github.com/keymapperorg/KeyMapper/issues/739) Long press triggers ignore constraints.

## [2.3.3](https://github.com/sds100/KeyMapper/releases/tag/v2.3.3)

#### 06 February 2022

- Update translations

### Bug fixes

- [#893](https://github.com/sds100/KeyMapper/issues/893) Creating intent actions with a boolean extra didn't work.
- [#885](https://github.com/keymapperorg/KeyMapper/issues/885) F-Droid build failed.
- [#894](https://github.com/keymapperorg/KeyMapper/issues/894) Links to documentation website broke.
- [#904](https://github.com/keymapperorg/KeyMapper/issues/904) Fix string.

## [2.3.2](https://github.com/sds100/KeyMapper/releases/tag/v2.3.2)

#### 31 January 2022

### Changes

- [#828](https://github.com/sds100/KeyMapper/issues/828) Rename the "Android 11 workaround" setting to be more clear
  what it does.
- [#859](https://github.com/sds100/KeyMapper/issues/859) Rename the "trigger from other apps" trigger option to be more
  clear what it does.
- [#753](https://github.com/sds100/KeyMapper/issues/753) Automatically add the "do not remap" trigger key option when
  remapping a modifier key. This will make sure the modifier key can still behave like a normal modifier key.

### Added

- [#814](https://github.com/sds100/KeyMapper/issues/814) Show system dialog to remove Key Mapper from battery
  optimisation so the user doesn't have to dig through their device sittings.

### Bug Fixes

- [#810](https://github.com/sds100/KeyMapper/issues/810) Intent actions didn't work
- [#789](https://github.com/sds100/KeyMapper/issues/789) Try to fix Key Mapper saying that the accessibility service has
  crashed even if it hasn't.
- [#829](https://github.com/sds100/KeyMapper/issues/829) Try to fix Key Mapper being listed as incompatible on Google
  Play for some devices.
- [#854](https://github.com/sds100/KeyMapper/issues/854) The toggle airplane mode action didn't work.

## [2.3.1](https://github.com/sds100/KeyMapper/releases/tag/v2.3.1)

#### 02 October 2021

### Changes
- [#772](https://github.com/sds100/KeyMapper/issues/772) Remapping game controllers should work automatically in games now. You no longer have to manually set the device of a key event action to be the game controller.

### Bug Fixes
- Try to fix a lot of random crashes on some devices.
- [#771](https://github.com/sds100/KeyMapper/issues/771) Don't show a "failed to find accessibility node" toast message when the open menu action fails.
- 
- [#775](https://github.com/sds100/KeyMapper/issues/775) The options for key maps would sometimes sporadically change when navigating the configuration screen.
- 
### Removed
- Option to give feedback by emailing the developer. The number of emails was overwhelming and most of them were not constructive at all.

## [2.3.0](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0)

These are all the changes from 2.2.0.

#### 27 August 2021

### Added
- 🎉 A new website with a tutorial! 🎉 [docs.keymapper.club](https://docs.keymapper.club)

- Action to broadcast intent, start activity and start service. #112
- Action to show the input method picker by using the Key Mapper keyboard. #531
- Action to toggle the notification drawer and the quick settings drawer. #242
- Action to call a phone number. #516
- Action to play a sound.

- A workaround for the Android 11 bug that sets the language of external keyboards to English-US when an accessibility service is enabled. #618 Read the guide here https://docs.keymapper.club/redirects/android-11-device-id-bug-work-around

- Prompt the user to read the quick start guide on the website the first time the app is opened. #544
- Links to a relevant online guide in each screen in the app. #539
- Option in key event action to input the key event through the shell. #559
- Splash screen #561
- Data migrations when restoring from backups. #574
- Enable hold down and disable repeat by default for modifier key actions. #579
- Ability to change the input method with the accessibility service on Android 11+. #619
- Make it clearer that selecting a screenshot to set up a tap coordinate action is optional. #632
- Show a prompt to install the Key Mapper GUI Keyboard when a key event action is created. #645
- Back up default key map settings in back ups. #659 
- Warnings when the accessibility service is turned on but isn't actually running. #643
- Show a message at the top of the home screen when mappings are paused. #642
- A caution message to avoid locking the user when using screen pinning mode. #602
- A logging page in the app which can be used instead of bug reports. #651
- A button in the settings to reset sliders to their default. #589
- A repeat limit action option. #663
- Show a dialog before resetting fingerprint gesture maps.
- A new Key Mapper keyboard that is designed for Android TV. #493
- An Intent API to pause/resume key maps. #668
- Allow Key Mapper to be launched from the Android TV launcher. #695
- Make it much easier to report bugs and turn off aggressive app killing. #728 There is now a button in the home screen menu to send a bug report and the user is now prompted to read dontkillmyapp.com when the accessibility service crashes.
- Support for repeat until limit reached action option in fingerprint gesture maps. #710

- Polish translations.
- Czech translations. 

### Changed
- Move action option to show a toast message to the same place as the vibrate option. #565
- Replace setting to choose Bluetooth device in settings with setting to choose any input device. #620
- Rename 'action count' option to 'how many times'. #611
- Move option to show the volume ui for an action to when the action is created. #639
- Tapping the pause/resume key maps notification now opens Key Mapper. #665
- Make action descriptions more descriptive when repeat is turned on. #666
- Alerts at the top of the home screen have been simplified.

### Removed
- Dex slide in the app intro because it didn't work. #646
- Buttons to enable all and disable all key maps in the home screen menu. #647
- Support for Android KitKat 4.4 and older. #627
- Ability to view changelog, license and privacy policy in an in-app dialog. They now open a link in the browser. #648
- Alerts at the top of the home screen to enable a Key Mapper keyboard, grant WRITE_SECURE_SETTINGS and grant Do not Disturb mode.

### Bug Fixes

See the 2.3.0 Beta releases below.

## [2.3.0 Beta 5](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0-beta.05)

#### 15 August 2021

### Changes
- Never show the "key mapper has crashed" dialog automatically since this causes a lot of confusion.
- Prompt the user to restart the accessibility service rather than report a bug. #736

### Added
- Polish translations

### Bug Fixes
- Just opening any options dialog might modify the options.
- Various NPEs
- Crash when showing on back pressed dialog
- Crash if trying to open an app store link without any app store being installed

## [2.3.0 Beta 4](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0-beta.04)

#### 19 July 2021

### Changes
- Don't show "key mapper has crashed" dialog the first time the app detects it has been crashed after being opened.

### Bug Fixes

- Write Secure Settings section in settings is enabled even if permission is revoked. #732
- Many random crashes. #744, #743, #742, #741, #740, #738, #737
- Don't crash when restoring back ups without a sounds folder in it.
- Don't restore a back up from a newer version of key mapper to prevent the app crashing when reading the restored data.

## [2.3.0 Beta 3](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0-beta.03)

#### 06 July 2021

### Added
- Make it much easier to report bugs and turn off aggressive app killing. #728 There is now a button in the home screen menu to send a bug report and the user is now prompted to read dontkillmyapp.com when the accessibility service crashes.
- Action to play a sound

### Bug Fixes
- Close notification drawer after the notification has been pressed. #719
- Crash if couldn't find input device. #730
- Crash if couldn't find chosen input method. #731
- Crash when failing to get package info. #721
- Crash if couldn't find Bluetooth device. #723
- Crash when disabling accessibility service. #720
- Reduce memory usage. #725
- Ensure log doesn't grow forever. #729

## [2.3.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0-beta.02)

#### 25 June 2021

### Added
- Support for repeat until limit reached action option in fingerprint gesture maps. #710

### Bug Fixes
- Crash on start up on some devices. #706
- Notification advertising fingerprint gesture maps is shown on every update #709
- Key map launcher shortcut repeats indefinitely when triggered if repeat until released is chosen. #707

## [2.3.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.3.0-beta.01) 

#### 22 June 2021

- A huge rewrite of the code which should make the app more stable and easier to add features in the future.

### Added
- 🎉 A new website with a tutorial! 🎉 [docs.keymapper.club](https://docs.keymapper.club)

- Action to broadcast intent, start activity and start service. #112
- Action to show the input method picker by using the Key Mapper keyboard. #531
- Action to toggle the notification drawer and the quick settings drawer. #242
- Action to call a phone number. #516
- A workaround for the Android 11 bug that sets the language of external keyboards to English-US when an accessibility service is enabled. #618 Read the guide here https://docs.keymapper.club/redirects/android-11-device-id-bug-work-around

- Prompt the user to read the quick start guide on the website the first time the app is opened. #544
- Links to a relevant online guide in each screen in the app. #539
- Option in key event action to input the key event through the shell. #559
- Splash screen #561
- Data migrations when restoring from backups. #574
- Enable hold down and disable repeat by default for modifier key actions. #579
- Ability to change the input method with the accessibility service on Android 11+. #619
- Make it clearer that selecting a screenshot to set up a tap coordinate action is optional. #632
- Show a prompt to install the Key Mapper GUI Keyboard when a key event action is created. #645
- Back up default key map settings in back ups. #659 
- Warnings when the accessibility service is turned on but isn't actually running. #643
- Show a message at the top of the home screen when mappings are paused. #642
- A caution message to avoid locking the user when using screen pinning mode. #602
- A logging page in the app which can be used instead of bug reports. #651
- A button in the settings to reset sliders to their default. #589
- A repeat limit action option. #663
- Show a dialog before resetting fingerprint gesture maps.
- A new Key Mapper keyboard that is designed for Android TV. #493
- An Intent API to pause/resume key maps. #668
- Allow Key Mapper to be launched from the Android TV launcher. #695

### Changed
- Move action option to show a toast message to the same place as the vibrate option. #565
- Replace setting to choose Bluetooth device in settings with setting to choose any input device. #620
- Rename 'action count' option to 'how many times'. #611
- Move option to show the volume ui for an action to when the action is created. #639
- Tapping the pause/resume key maps notification now opens Key Mapper. #665
- Make action descriptions more descriptive when repeat is turned on. #666
- Alerts at the top of the home screen have been simplified.

### Removed
- Dex slide in the app intro because it didn't work. #646
- Buttons to enable all and disable all key maps in the home screen menu. #647
- Support for Android KitKat 4.4 and older. #627
- Ability to view changelog, license and privacy policy in an in-app dialog. They now open a link in the browser. #648
- Alerts at the top of the home screen to enable a Key Mapper keyboard, grant WRITE_SECURE_SETTINGS and grant Do not Disturb mode.

### Bug fixes
- Fix jank #549
- Fix text consistency #543
- A parallel trigger which contains another parallel trigger after the first key should cancel the other. #571
- Actions go off screen for key maps on the home screen. #613
- Remove uses of Android framework strings for dialog buttons. #650 
- Trigger key click type sometimes resets to short press. #615
- Wrong device id is used when performing key event actions and there are multiple devices with the same descriptor. #637
- Trigger key isn't imitated after a failed double press. #606
- Actions don't start repeating on a failed long press or failed double press. #626 
- Crash when modifying a huge number of key maps. #641
- Home menu is chopped off on screens with small height. #582
- Crash when double pressing button to open action or trigger key options. #600
- Some action options disappear when adding a new trigger key. #594
- An action can continue to repeat even when the trigger is released if delay until next action is not 0. #662
- A lot of input latency when using a lot of constraints. #599
- Trigger button isn't imitated when a short press trigger with multiple keys fails to be triggered. #664
- Overlapping triggers. #653

## [2.2.0](https://github.com/sds100/KeyMapper/releases/tag/v2.2.0) 

#### 07 March 2021

This sums up all the changes for 2.2

### Added
- Remap fingerprint gestures! #378 Android 8.0+ and only on devices which support them. Even devices with the setting to swipe down for notifications might not support this! The dev can't do anything about this.

- Widget/shortcut to launch actions. #459
- Setting to show the first 5 digits of input devices so devices with the same name can be differentiated in Key Mapper lists. #470
- Show a warning at the top of the homescreen if the user hasn't disabled battery optimisation for Key Mapper. #496
- Action option to hold down until the trigger is pressed again. #479
- Action option to change the delay before the next action in the list. #476
- Orientation constraint. #505
- Key Event action option to pretend that the Key Event came from a particular device. #509
- Use duplicates of the same key in a sequence trigger. #513
- Show the fingerprint gesture intro slide when updating to 2.2 #545
- Show a silent notification, which advertises the remapping fingerprint gesture feature, when the user updates to 2.2 #546
- Trigger key maps from an Intent #490
- Prompt the user to go to https://dontkillmyapp.com when they first setup the app.
- Add Fdroid link to the Key Mapper GUI Keyboard ad. #524

### BREAKING CHANGES
- Key Mapper action shortcuts work completely differently. See https://docs.keymapper.club/user-guide/triggers/#trigger-from-other-apps-230

### Changes
- No max limit for sliders (except in settings). #458
- The app intro slides will show feedback if the steps have been done correctly.

### Removed
- XDA Labs links because it has been shut down.

### Bug Fixes
- Save and restore state for all view models. #519
- Use View Binding in fragments properly. This should stop random crashes for some users. #518
- Hold Down action option doesn't work for long press triggers. #504
- A trigger for a specific device can still be detected if the same buttons on another device are pressed. #523
- Fix layout of the trigger fragment on some screen sizes so that some things aren't cut off. #522
- Remapping modifier keys to the same key didn't work as expected. #563
- Parallel triggers which contained another parallel trigger didn't cancel the other. #571
- Don't allow screen on/off constraints for fingerprint gestures #570
- Rename Key Mapper CI Keyboard to Key Mapper CI Basic Input Method.
- Notifications had no icon on Android Lollipop.
- remove coloured navigation bar on Android Lollipop.
- Hold Down option wasn't allowed on Android 8.0 or older.
- Detecting whether remapping fingerprint gestures are supported didn't work.
- The flashlight action would sometimes crash the app.
- The error message for an app being disabled was the wrong one.
- Actions to open Android TV apps didn't work #503
- The app list didn't show Android TV-only apps. #487
- Settings for repeat rate and delay until repeat didn't match their names when configuring an action.
- Text would move up/down when sliding between slides in the app intro. #540
- Icon for "specific app playing media" constraint had the wrong tint. #535
- Limit Media actions to Android 4.4 KitKat+ because they don't work on older versions.
- Up Key Event was sent from all keymaps with the "hold down" action option regardless of whether the trigger was released. #533
- Testing actions didn't work.
- Scroll position was lost when reloading the key map list.
- Try to fix random crashes when navigating.
- Duplicating key maps didn't work.

## [2.2.0 Beta 2](https://github.com/sds100/KeyMapper/releases/tag/v2.2.0-beta.2) 

#### 29 Jan 2021

### Added
- Remap fingerprint gestures! #378 Android 8.0+ and only on devices which support them. Even devices with the setting to swipe down for notifications might not support this! The dev can't do anything about this.
- Show the fingerprint gesture intro slide when updating to 2.2 #545
- Show a silent notification, which advertises the remapping fingerprint gesture feature, when the user updates to 2.2 #546
- Trigger key maps from an Intent #490
- Prompt the user to go to https://dontkillmyapp.com when they first setup the app.
- Add Fdroid link to the Key Mapper GUI Keyboard ad. #524

### BREAKING CHANGES
- Key Mapper action shortcuts work completely differently. See https://docs.keymapper.club/user-guide/triggers/#trigger-from-other-apps-230

### Changes
- The app intro slides will show feedback if the steps have been done correctly.

### Removed
- XDA Labs links because it has been shut down.

### Bug Fixes
- Remapping modifier keys to the same key didn't work as expected. #563
- Parallel triggers which contained another parallel trigger didn't cancel the other. #571
- Don't allow screen on/off constraints for fingerprint gestures #570
- Rename Key Mapper CI Keyboard to Key Mapper CI Basic Input Method.
- Notifications had no icon on Android Lollipop.
- remove coloured navigation bar on Android Lollipop.
- Hold Down option wasn't allowed on Android 8.0 or older.
- Detecting whether remapping fingerprint gestures are supported didn't work.
- The flashlight action would sometimes crash the app.
- The error message for an app being disabled was the wrong one.
- Actions to open Android TV apps didn't work #503
- The app list didn't show Android TV-only apps. #487
- Settings for repeat rate and delay until repeat didn't match their names when configuring an action.
- Text would move up/down when sliding between slides in the app intro. #540
- Icon for "specific app playing media" constraint had the wrong tint. #535
- Limit Media actions to Android 4.4 KitKat+ because they don't work on older versions.
- Up Key Event was sent from all keymaps with the "hold down" action option regardless of whether the trigger was released. #533
- Testing actions didn't work.
- Scroll position was lost when reloading the key map list.
- Try to fix random crashes when navigating.
- Duplicating key maps didn't work.

## [2.2.0 Beta 1](https://github.com/sds100/KeyMapper/releases/tag/v2.2.0-beta.1) 

#### 30 Dec 2020

### Added
- Remap fingerprint gestures! #378 Android 8.0+ and only on devices which support them. Even devices with the setting to swipe down for notifications might not support this! The dev can't do anything about this.

- Widget/shortcut to launch actions. #459
- Setting to show the first 5 digits of input devices so devices with the same name can be differentiated in Key Mapper lists. #470
- Show a warning at the top of the homescreen if the user hasn't disabled battery optimisation for Key Mapper. #496
- Action option to hold down until the trigger is pressed again. #479
- Action option to change the delay before the next action in the list. #476
- Orientation constraint. #505
- Constraint for when a specific app is playing media. #508
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
