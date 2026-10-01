# Third-Party Notices

Anki Deck Builder is licensed under the [MIT License](LICENSE.md). This document records compatibility information and the separate license terms of third-party software used by the project. It does not change the license of Anki Deck Builder.

## Anki compatibility notice

This software is independently developed for the limited purpose of creating Anki-compatible `.apkg` files. Users may import those files into a separately obtained installation of Anki.

This software does not bundle, modify, launch, or distribute the Anki application, and it does not use Anki source code as an application dependency. Anki is therefore not listed below as a bundled third-party component. The Anki application and its source code remain subject to their own license terms, which are available in the [official Anki repository](https://github.com/ankitects/anki/blob/main/LICENSE).

Anki Deck Builder is not affiliated with or endorsed by the Anki project. The name "Anki" is used only to describe compatibility with the package format and the intended destination application. No Anki logo or other Anki project asset is included by this notice.

## Third-party software

Third-party software remains subject to its own license terms. The following table records the primary libraries and build tools currently declared by the project.

| Project | Version used | License | License reference |
| --- | --- | --- | --- |
| Kotlin, Kotlin Gradle plugins, and Kotlin Test | 2.4.10 | Apache License 2.0 | [Kotlin license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) |
| Compose Multiplatform | 1.11.1 | Apache License 2.0 | [Compose Multiplatform license](https://github.com/JetBrains/compose-multiplatform/blob/master/LICENSE.txt) |
| Compose Material 3 | 1.11.0-alpha07 | Apache License 2.0 | [AndroidX license](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) |
| AndroidX Lifecycle for Compose | 2.11.0-beta01 | Apache License 2.0 | [AndroidX license](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) |
| Room | 3.0.1 | Apache License 2.0 | [AndroidX license](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) |
| AndroidX SQLite | 2.7.0 | Apache License 2.0 | [AndroidX license](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) |
| SQLite | Bundled by AndroidX SQLite 2.7.0 | Public domain | [SQLite copyright](https://www.sqlite.org/copyright.html) |
| Decompose and Essenty | 3.5.0 / 2.5.0 | Apache License 2.0 | [Decompose license](https://github.com/arkivanov/Decompose/blob/master/LICENSE), [Essenty license](https://github.com/arkivanov/Essenty/blob/master/LICENSE) |
| kotlinx.coroutines | 1.11.0 | Apache License 2.0 | [kotlinx.coroutines license](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) |
| kotlinx-datetime | 0.8.0 compatibility artifact (`0.8.0-0.6.x-compat`) | Apache License 2.0 | [kotlinx-datetime license](https://github.com/Kotlin/kotlinx-datetime/blob/master/LICENSE.txt) |
| Klogging | 0.11.8 | Apache License 2.0 | [Klogging license](https://github.com/klogging/klogging/blob/main/LICENSE.txt) |
| MVIKotlin | 4.4.0 | Apache License 2.0 | [MVIKotlin license](https://github.com/arkivanov/MVIKotlin/blob/master/LICENSE) |
| Kotlin Symbol Processing (KSP) | 2.3.11 | Apache License 2.0 | [KSP license](https://github.com/google/ksp/blob/main/LICENSE) |
| Roborazzi | 1.70.0 | Apache License 2.0 | [Roborazzi license](https://github.com/takahirom/roborazzi/blob/main/LICENSE) |
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 | [JUnit 4 license](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt) |
| Gradle Wrapper and Gradle build tool | 9.3.1 | Apache License 2.0 | [Gradle license](https://github.com/gradle/gradle/blob/master/LICENSE) |
| Kover | 0.9.9 | Apache License 2.0 | [Kover license](https://github.com/Kotlin/kotlinx-kover/blob/main/LICENSE) |
| Detekt | 2.0.0-alpha.6 | Apache License 2.0 | [Detekt license](https://github.com/detekt/detekt/blob/main/LICENSE) |
| ktlint | 1.8.0 | MIT License | [ktlint license](https://github.com/ktlint/ktlint/blob/master/LICENSE) |
| ktlint Gradle plugin | 14.2.0 | MIT License | [ktlint Gradle plugin license](https://github.com/JLLeitschuh/ktlint-gradle/blob/main/LICENSE.txt) |

The license references above are provided for convenience. The license and notice files distributed with each dependency are authoritative. Dependencies may also bring transitive software with additional notices or license terms. Before distributing an application package, the resolved dependency graph must be reviewed and all notices required by the packaged software must be included with the release.

This document must be updated whenever a third-party library or build tool is added, removed, or upgraded. Planned dependencies are not listed until they are added to the project.
