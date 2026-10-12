@file:OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)

plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.room)
}

compose.resources {
    packageOfResClass = "reframecv.shared.generated.resources"
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
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
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.klogging)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        getByName("jvmTest").resources.srcDir(rootProject.file("docs/schemas"))
        jvmTest.dependencies {
            implementation(libs.jsonSchemaValidator)
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.roborazzi.composeDesktop)
        }
    }
}

dependencies {
    add("kspJvm", libs.room.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    outputDir.set(file("src/jvmTest/screenshots"))
    compare {
        outputDir.set(layout.buildDirectory.dir("outputs/roborazzi-comparison"))
    }
}
