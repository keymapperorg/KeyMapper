## Consistency Standards

### Writing

It is important to use consistent language within the app's code and documentation. Make sure that your submissions comply with these standards. If you have noticed inconsistencies you can alert the developer with [an issue](https://github.com/sds100/KeyMapper/issues/new) or in [the Discord](http://keymapper.club). These standards don't need to be followed in places other than the documentation and source code. Follow this Material Design [guide](https://material.io/design/communication/writing.html).

#### Case

No title case unless it is the title of a webpage, section heading. No title case in the string resources in the source code at all.

Here are words and phrases that can be spelt or capitalised in multiple ways.

| Terminology           | Correct 😍                                                    | Incorrect 👿                        |
| --------------- | ------------------------------------------------------------ | ---------------------------------- |
| The app's name. | **Key Mapper**<br />Use **Keyboard/Button Mapper** when advertising the app as this is how it appears in the app stores. You can use "Key Mapper" in the rest of the advert since this is much shorter. | key mapper, keymapper              |
| A key map       | **key map**. In variable and class names it should be keymap, fooKeymap instead of fooKeyMap. The class KeyMap is an exception. | keymap, Key Map. key-map. Key-Map. |
| A trigger       | **trigger**                                                  | Trigger                            |
| An action       | **action**                                                   | Action                             |
| A constraint    | **constraint**                                               | Constraint                         |
| A key event | **key event** | Key Event, keyevent |
| A key code | **key code**. In variable and class names it should be keycode instead of keyCode. | keycode, Key Code |
| A home screen | **home screen** | homescreen, Home Screen |
| A backup/ to back up | Noun: **backup**. Verb; **back up** | Backup when using the verb. |

### Documentation

#### Headings

If a heading is for a particular feature then it should include the minimum supported Key Mapper version except version 2.0.* because 1.* isn't supported at all and whether it requires root.

E.g "Trigger When Screen is Off (ROOT, 2.1.0+)"

### Committing

### Versioning

### Code

Follow Google's Kotlin style guide. [https://developer.android.com/kotlin/style-guide](https://developer.android.com/kotlin/style-guide)
