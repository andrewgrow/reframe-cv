plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
}

val ktlintVersion = libs.versions.ktlint.get()

allprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(ktlintVersion)
            filter {
                exclude {
                    val path = it.file.invariantSeparatorsPath
                    path.contains("/build/") || path.contains("/generated/")
                }
            }
        }
    }
}

detekt {
    source.setFrom("shared/src", "desktopApp/src")
    config.setFrom("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    basePath.set(projectDir)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    include("**/*.kt")
    exclude("**/build/**", "**/generated/**")
}

dependencies {
    kover(project(":shared"))
    kover(project(":desktopApp"))
}

kover {
    reports {
        total {
            verify {
                rule {
                    minBound(75)
                }
            }
        }
        filters {
            excludes {
                packages("ankideckbuilder.shared.generated.resources")
                classes(
                    "ankideckbuilder.database.AppDatabase_Impl*",
                    "ankideckbuilder.database.AppDatabaseConstructor*",
                    "ankideckbuilder.database.project.ProjectDao_Impl*",
                )
            }
        }
    }
}
