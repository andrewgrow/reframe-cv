plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(project(":recognition-api"))
            api(libs.ktor.client.core)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies { implementation(libs.kotlin.testJunit) }
    }
}
