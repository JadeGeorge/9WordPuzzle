// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Redirect all build output outside OneDrive to prevent file-locking errors during incremental builds.
allprojects {
    layout.buildDirectory.set(rootDir.resolve("F:/NineLettersBuild/${rootProject.name}/$name"))
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}