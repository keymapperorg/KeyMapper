:material-checkbox-marked:{: .accent-light } &nbsp; Detect trigger when screen is off. ROOT only.

These are the buttons which can be detected when the screen is off. Let the developer know about any buttons you would
also like to be supported.

!!! note 
    Please send the output of pressing the buttons while running the `adb shell getevent -lq` command so the
    developer knows which key event name they need to add.

* Volume Up
* Volume Down
* Headset button
* Camera focus button
* Camera button
* Bixby button - on some ROMs the Bixby button is mapped to the Menu or Assist key code in Android so it will appear as these buttons in the trigger.
* Search button