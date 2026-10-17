@file:OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)

plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
}

compose.resources {
    packageOfResClass = "reframecv.shared.generated.resources"
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.animation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.decompose)
            implementation(libs.decompose.extensionsCompose)
            api(libs.mvikotlin.core)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.logging)
            api(libs.mvikotlin.coroutines)
            implementation(libs.kotlinx.coroutinesCore)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(project(":network"))
            implementation(project(":providers"))
            implementation(project(":database"))
            implementation(libs.klogging)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        getByName("jvmTest").resources.srcDir(rootProject.file("docs/schemas"))
        jvmTest.dependencies {
            implementation(project(":database"))
            implementation(project(":network"))
            implementation(project(":providers"))
            implementation(libs.jsonSchemaValidator)
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.roborazzi.composeDesktop)
        }
    }
}

roborazzi {
    outputDir.set(file("src/jvmTest/screenshots"))
    compare {
        outputDir.set(layout.buildDirectory.dir("outputs/roborazzi-comparison"))
    }
}
