# Notepad Plus Android V3

Clean-room native Android Notepad++-style editor starter project.

## V3 build changes

- Java-only application code.
- No Kotlin Gradle plugin.
- No explicit Kotlin stdlib dependencies.
- AndroidX AppCompat 1.7.0 only.
- Java 17.
- Compile/target SDK 35.
- `SelectionAwareEditText` is used consistently.
- No invalid horizontalScrollBarEnabled/verticalScrollBarEnabled XML attributes.
- Multiple tabs, New/Open/Save/Save As, Find/Replace, line numbers, cursor status, theme toggle.

## Important

If Android Studio shows Kotlin stdlib artifacts in this project, they are being introduced by another project-level dependency/cache configuration rather than by this app's `app/build.gradle`.

## Build

Open the folder containing `settings.gradle` in Android Studio.

Then:
Build > Clean Project
Build > Rebuild Project

APK:
app/build/outputs/apk/debug/app-debug.apk
