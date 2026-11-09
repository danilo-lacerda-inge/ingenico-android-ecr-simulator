plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.ingenico.ecrsimulator.shared"
        compileSdk = 37
        minSdk = 30
    }

    jvm("desktop")

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
