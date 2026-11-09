plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":connector-client"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

tasks.processResources {
    from(rootProject.file("app/src/main/res/drawable-nodpi"))
}

tasks.withType<JavaExec>().configureEach {
    debugOptions.host.set("127.0.0.1")
}

compose.desktop {
    application {
        mainClass = "com.ingenico.ecrsimulator.desktop.MainKt"
    }
}
