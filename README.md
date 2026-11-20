# Walking Distance Calculator

This is a separate Android Studio project and app, independent of Activity Tracker.
It converts walking duration to distance at an adjustable speed, and supports
time/speed presets, copying a result, and a multi-segment session log.

Open this project folder in Android Studio, let Gradle sync, and run the `app`
configuration. The project requires Android SDK 37 and a JDK supported by
Android Gradle Plugin 9.5.1. Android Studio can install missing SDK components
during sync.

To build a debug APK on Windows, run `gradlew.bat assembleDebug` in this folder.
The output is `app\build\outputs\apk\debug\app-debug.apk`. The application ID is
`com.amica.walkingdistance`, so it can be installed alongside Activity Tracker.
The debug APK is intended for testing, not as a signed release.

Session entries are held in memory, like the original calculator; they survive
screen rotation but are not saved after the app process ends. Reset changes the
current inputs without clearing the session; use Clear All to clear the log.
