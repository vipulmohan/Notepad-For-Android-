# GitHub Actions APK build

1. Create an empty GitHub repository.
2. Upload the contents of this folder to the repository root.
3. On GitHub, open **Actions**.
4. Select **Build Android APK**.
5. Choose **Run workflow**.
6. When it finishes, open the workflow run and download the artifact:
   `NotepadPlusAndroidV3-debug`
7. The artifact contains `app-debug.apk`.

The workflow uses Temurin JDK 17 and Gradle 8.9 in a clean Ubuntu runner.
It installs Android SDK platform 35 and build tools 35.0.0.

This is deliberately independent of your local Java 8/Gradle/Kotlin setup.
