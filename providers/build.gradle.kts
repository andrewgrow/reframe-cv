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
            implementation(project(":network"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.kotlinx.datetime)
        }
        getByName("jvmMain").resources.srcDir(rootProject.file("docs/schemas"))
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
