import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(libs.klogging)
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.decompose)
    implementation(libs.decompose.extensionsCompose)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kotlinx.coroutinesCore)
    testImplementation(libs.kotlin.testJunit)
}

compose.desktop {
    application {
        mainClass = "reframecv.MainKt"
        if (System.getProperty("os.name").startsWith("Mac")) {
            jvmArgs("-Xdock:name=ReframeCV", "-Dapple.awt.application.name=ReframeCV")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ReframeCV"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("icons/reframe-cv.icns"))
            }
            windows {
                iconFile.set(project.file("icons/reframe-cv.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/reframe-cv.png"))
            }
        }
    }
}
