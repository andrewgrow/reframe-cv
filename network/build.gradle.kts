plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.kotlinx.serializationJson)
            api(libs.ktor.client.core)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.jsonSchemaValidator)
        }
        getByName("jvmTest").resources.srcDir(rootProject.file("docs/schemas"))
    }
}
