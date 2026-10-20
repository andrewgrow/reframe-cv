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
            api(libs.kotlinx.coroutinesCore)
            api(libs.kotlinx.serializationJson)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        jvmTest.dependencies { implementation(libs.kotlin.testJunit) }
    }
}
