## Becoming a tester

You can get the apks for the pre-release versions in 2 ways:

1. Join [the Discord server](http://keymapper.club) and download the apks from the #testing-builds channel.
2. Download the apk from GitHub Actions. This requires a GitHub account.
    You can get alpha builds [here](https://github.com/sds100/KeyMapper/actions/workflows/android.yml) and beta builds [here](https://github.com/sds100/KeyMapper/actions/workflows/android-release.yml).

    Click on a 'workflow run' and then scroll down to see the 'artifacts'. If a build was successful then you can find the apk here.

!!! info
    All testing builds have `.ci.X` at the end of the version name where 'X' is a number is incremented every time a new build is made. Builds are made when a new feature or bug fix is implemented.

    There are two types of pre-release versions:

    - **Alpha**.  These have ".alpha" in the version name and are the most unstable. Expect the most crashes and broken features in these builds. BEWARE! Your data in Key Mapper isn't considered compatible between alpha builds so it is possible that Key Mapper will crash and refuse to fix itself.
    - **Beta**. These builds have some of the latest features and contain a few bugs. You can safely update between versions. These have ".beta.X" in the version name. These are pre-release builds for the the open-testing channel on Google Play and F-droid always has beta builds. When all known bugs are fixed a new build is released to the app stores.

### How can I help?
- Test and experiment new features. All features and bug-fixes that are being worked on for a release can be found on the Projects page [here](https://github.com/sds100/KeyMapper/projects).
- If you find any bugs or crashes then report them by following the guide [here](report-issues.md).

## Contributing code

### Introduction to the structure

This app follows Clean Architecture and package-by-feature.

#### Architecture

All data structures that are persisted are passed around as one of two objects:

1. **Non-Entity**. This models the data in a way that makes the code more readable and doing the business logic easier. There are no rules for how these need to be named. They should be named what they are. E.g KeyMap, Action, Constraint.

2. **Entity**. This models how the data should be stored. The class name has an ...Entity suffix. E.g KeyMapEntity. The data is more optimised for storing and the code required to get the data from these models isn't very concise or elegant. The developer took some strange decisions in the first versions of this app. 😆

Every screen in the app has a view model and the view model interacts   with one or multiple *use cases* (more below). The view model converts data that needs to be shown to the user into something that can populate the user interface. For example, the data values in the Action object isn't very useful to the user so this needs to be converted into strings and images that do mean something to the user. All the view models have a ResourceProvider dependency which is how they get strings, Drawables and colours from the resources without having to use a Context. This isn't a problem for configuration changes (e.g locale change) because the activity is recreated, which means all the resources are re-fetched in the view model.

The use cases contains all the business logic in the app. A *use case*  interacts with the adapters and repositories mentioned below. A use case is made for everything that can be done in the app. E.g configuring a key map, displaying a mapping, configuring settings, onboarding the user. Most use cases correspond to something that *the user can do* in the app but some do not because they contain complicated code that is used in multiple use cases. E.g the GetActionErrorUseCase which determines if an action can be performed successfully.

Adapters and repositories contain all interactions with the Android framework (except UI stuff). This is so that tests can be more easily written and executed. Android often changes what apps are allowed to do and how so abstracting these interactions away means the code only needs to be changed in a single place. This means that the only place that a Context object is used is in Services, Activities, Fragments and the adapters.

#### Package by feature

Every package contains files related to each other. For example, everything (view models, fragments, use cases) to do with constraints is stored in one package.
The only package which isn't a feature is the `data` package because it is useful to have some of the things in there together, e.g the migrations.
The `system` package bundles all the packages which are related to the Android framework because there are so many.

![contributing-app-structure](images/contributing-app-structure.png)

### Setting up the environment

1. Fork the KeyMapper repository (repo).
2. Clone the repo to your device. It will clone to a folder called KeyMapper by default.
3. [Install](https://developer.android.com/studio/install) Android Studio if you don't have it already. It is available for Windows, Linux and macOS.
4. Open the cloned KeyMapper folder in Android Studio. Install anything Android Studio prompts you to install. E.g the gradle wrapper version used by KeyMapper or older Android SDK versions.
5. Create a new branch off develop which begins with "feature/" if it is a new feature or "fix/" if it is a bug fix. Then put a brief description of the feature/bug fix.
6. Make any changes then commit them to your forked repo then make a pull request!

### Branches 🌴

 - master: Everything in the latest stable release.
 - develop: The most recent changes. The app is potentially unstable but it can be successfully compiled. Merges into a release branch when enough has been changed for a new release.

 - release/*: Branched off develop. Beta releases for a particular release are compiled from here. Once the code is stable, it will be merged into master. No big changes should be made/merged here as the purpose of this branch is to make a release stable. By separating upcoming releases from develop, new features can be worked on in develop without affecting the upcoming release's code base.
 - feature/*: Any new changes currently being developed. Merges into develop.
 - hotfix/*: Any small, quick (atleast things which SHOULD be quick) changes that need to be made. Merge into develop and release. If there is no release already being worked on, quickly release a new version depending on how critical the issue is and merge the new release branch into master.


### Committing

Format:
```
<issue id> <type>: <subject>
```

Every feature or bug fix commit should have an issue associated with it. This is a cue for the developer to plan what they are doing which improves efficiency. A feature should be split up into multiple tasks and each task should have its own commit. The feature should be developed on a separate branch and then merged into develop.

#### Example
```
#100 feat: This a new feature
```

#### Types
- feat: a new feature
- fix: a bug fix
- docs: changes to documentation
- style: formatting, missing semi colons, etc; no code change
- refactor: refactoring production code
- test: adding tests, refactoring test; no production code change
- chore: updating build tasks, package manager configs, version name changes, etc; no production code change
- release: a new release.
- website: stuff to do with the website.

The README, License, Credits, Changelog and Privacy Policy files should just be changed in the master branch.

### Versioning

This project uses semantic versioning. e.g 1.2.3-alpha.01

- 1st digit: major release. Only incremented when a big enough change happens to the project.
- 2nd digit: minor releases. Incremented when a new feature or a few are added.
- 3rd digit: patches. Incrememtend after a hotfix or bug fix.

Additional labels can be used as a suffix. e.g "alpha".

The version code in the develop branch should always be the highest. Therefore, when a new version is released in the release branch, it should be incremented as well.

### Code Style

Follow Google's Kotlin style guide. [https://developer.android.com/kotlin/style-guide](https://developer.android.com/kotlin/style-guide)

## Translating 🌍
You can translate this project on the [CrowdIn page](https://crowdin.com/project/key-mapper). Translations will be merged into production once everything has been translated. If your language isn't available on the CrowdIn page then contact the developer so we can add it. Our contact details are in the footer of every page on this site.

We really appreciate translators so thank you! 🙂

## Consistency Standards

### Writing

It is important to use consistent language within the app's code and documentation. Make sure that your submissions comply with these standards. If you have noticed inconsistencies you can alert the developer with [an issue](https://github.com/sds100/KeyMapper/issues/new) or in [the Discord](http://keymapper.club). These standards don't need to be followed in places other than the documentation and source code. Follow this Material Design [guide](https://material.io/design/communication/writing.html).

#### Case

No title case unless it is the title of a webpage, section heading. No title case in the string resources in the source code at all.

Here are words and phrases that have been spelt inconsistently by the developer and everyone should follow these guidelines. 

| Terminology           | Correct 😍                                                    | Incorrect 👿                        |
| --------------- | ------------------------------------------------------------ | ---------------------------------- |
| The app's name. | **Key Mapper**<br />Use **Keyboard/Button Mapper** when advertising the app as this is how it appears in the app stores. You can use "Key Mapper" in the rest of the advert since this is much shorter. | key mapper, keymapper              |
| A key map       | **key map**. KeyMap, keyMap in variable and class names. | keymap, Key Map. key-map. Key-Map. |
| A trigger       | **trigger**                                                  | Trigger                            |
| An action       | **action**                                                   | Action                             |
| A constraint    | **constraint**                                               | Constraint                         |
| A key event | **key event** | Key Event, keyevent |
| A key code | **key code**. In variable and class names it should be keyCode instead of keycode. | keycode, Key Code |
| A home screen | **home screen** | homescreen, Home Screen |
| A backup/ to back up | Noun: **backup**. Verb; **back up** | Backup when using the verb. |

### Documentation

#### Headings

If a heading is for a particular feature then it should include the minimum supported Key Mapper version except version 2.0.

E.g "Trigger When Screen is Off (ROOT, 2.1.0+, Android 8.1+)" or "A feature (up to Android 10)".
