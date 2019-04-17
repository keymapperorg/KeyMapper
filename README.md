# KeyMapper

Key Mapper is a free and open source Android app that maps a single or multiple key events to a custom action.

[XDA Developers thread](https://forum.xda-developers.com/android/apps-games/app-keyboard-button-mapper-t3914005)

![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/sds100/KeyMapper.svg)
![GitHub Releases Downloads](https://img.shields.io/github/downloads/sds100/keymapper/total.svg?label=GitHub%20Releases%20Downloads)
![GitHub release](https://img.shields.io/github/release/sds100/KeyMapper.svg)

![](app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png?raw=true) <a href='https://play.google.com/store/apps/details?id=io.github.sds100.keymapper&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_gb/badges/images/generic/en_badge_web_generic.png' height=128px/> </a>

### Help
Look at the help page in the wiki.

### Translations 🌍
Take a look at the translations page in the wiki. Any translations would be appreciated. 😊

### Branches 🌴
 - master: Everything in the latest stable release.
 - develop: The most recent changes. The app is potentially unstable but it can be successfully compiled. Merges into a release branch when enough has been changed for a new release.

 - release/*: Branched off develop. Beta releases for a particular release are compiled from here. Once the code is stable, it will be merged into master. No big changes should be made/merged here as the purpose of this branch is to make a release stable. By separating upcoming releases from develop, new features can be worked on in develop without affecting the upcoming release's code base.
 - feature/*: Any new changes currently being developed. Merges into develop.
 - hotfix/*: Any small, quick (atleast things which SHOULD be quick) changes that need to be made. Merge into develop and release. If there is no release already being worked on, quickly release a new version depending on how critical the issue is and merge the new release branch into master.

### Versioning
This project uses semantic versioning. e.g 1.2.3-alpha.1

- 1st digit: major release. Only incremented when a big enough change happens to the project.
- 2nd digit: minor releases. Incremented when a new feature or a few are added.
- 3rd digit: patches. Incrememtend after a hotfix or bug fix.

Additional labels can be used as a suffix. e.g "alpha".

#### Version codes
The version code in the develop branch should always be the highest. Therefore, when a new version is released in the release branch, it should be incremented as well.

The README, License, Credits, Changelog and Privacy Policy files should just be changed in the master branch.
