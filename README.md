# ECR Simulator

## Set up and run in Android Studio

### Requirements

- Android Studio with support for the Android Gradle Plugin version used by this project.
- JDK 17 or newer. Android Studio's bundled JDK can be selected in Gradle settings.
- Android SDK Platform 37. The app supports Android 11 (API 30) and newer.

### Open and run

1. Clone or download this repository.
2. In Android Studio, choose **File > Open** and select the repository's root folder (the folder containing `settings.gradle.kts`).
3. Allow Android Studio to sync the project and download any missing Gradle or SDK components. If prompted, install Android SDK Platform 37 using **Tools > SDK Manager**.
4. If Gradle reports a Java version issue, open **Settings > Build, Execution, Deployment > Build Tools > Gradle** and set **Gradle JDK** to JDK 17 or newer (the bundled JDK is suitable).
5. Select the **app** run configuration and an Android device or emulator running API 30 or newer, then click **Run**.

To create an emulator, open **Tools > Device Manager**, create a virtual device, and choose a system image with API 30 or newer.
