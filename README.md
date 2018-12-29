# KeyMapper
An Android app that maps a single or multiple key events to a custom action.

### Help
Look at the help page in the wiki.

### Branches
 - master: Everything in the latest stable release.
 - develop: The most recent changes. The app is potentially unstable but it can be successfully compiled. Merges into master when in a stable state.
 - release/*: Branched off develop. Beta releases for a particular release are compiled from here. Once the code is stable, it will be merged into master. No big changes should be made/merged here as the purpose of this branch is to make a release stable. By separating upcoming releases from develop, new features can be worked on in develop without affecting the upcoming release's code base.
 - feature/*: Any new changes currently being developed. Merges into develop.
 - hotfix/*: Any small, quick (atleast things which SHOULD be quick) changes that need to be made. Merge into master, develop and release.

### Versioning
This project uses semantic versioning. e.g 1.2.3-alpha.1

- 1st digit: major release. Only incremented when a big enough change happens to the project.
- 2nd digit: minor releases. Incremented when a new feature or a few are added.
- 3rd digit: patches. Incrememtend after a hotfix or bug fix.

Additional labels can be used as a suffix. e.g "alpha".
