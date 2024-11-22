# Gramophone

A sane music player built with media3 and material design library that is following android's standard strictly.

## Features
- Up-to-date material 3 design
- Monet themed icon on Android 12+
- Dynamic player UI monet color
- View and play your favorite music
- Search your favourite music
- Uses MediaStore to quickly access music database
- Synced lyrics
- Read-only Playlist support

## Building
To build this app, you will need the latest beta version of [Android Studio](https://developer.android.com/studio) and a fast network.

### Set up package type
Gramophone has a package type that indicates the source of the application package. Package type string is extracted from an external file named `package.properties`.

Simply open your favorite text editor, type `releaseType=SelfBuilt`, and save it in the root folder of the repository as `package.properties`.

After this, launch Android Studio and import your own signature. You should be able to build Gramophone now.

## License
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](https://github.com/AkaneTan/Gramophone/blob/beta/LICENSE) file for details.
