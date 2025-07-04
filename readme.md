# Gramophone

A sane music player built with media3 and the Material Design library that is following Android's standard strictly.

## Features
- Light, stable, and minimalistic
- Up-to-date Material 3 design and theme
  - Dynamic player UI Monet color
  - Monet themed icon (Android 12+)
- Music library features
  - Search and browse for your favourite music
  - Uses MediaStore to quickly access on-device music
  - List and grid views
  - Browse songs by folder and filesystem
  - Natural sorting and various other sorting options
  - Read-only playlist support
- Synced lyrics
  - LRC, TTML, SRT
  - Supports word/syllable Karaoke lyrics synchronization
- Full support for ReplayGain 2.0
- Support for system/third-party Equalizer apps

## Building
To build this app, you will need the latest beta version of [Android Studio](https://developer.android.com/studio) and a fast network.

### 1. Submodules

Gramophone includes certain dependencies such as media3 as git submodule. Make sure you download git submodules by running `git submodule update --init --recursive` before trying to build Gramophone.

### 2. Set up package type
Gramophone has a package type that indicates the source of the application package. Package type string is extracted from an external file named `package.properties`.

Simply open your favorite text editor, type `releaseType=SelfBuilt`, and save it in the root folder of the repository as `package.properties`.

### 3. Start the build
Launch Android Studio and import your own app signing signature. You should be able to build Gramophone now.

## License
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](https://github.com/FoedusProgramme/Gramophone/blob/beta/LICENSE) file for details.
