plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            api(libs.ktor.client.core)
            implementation(libs.kotlinx.serializationJson)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.klogging)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}
