# ReframeCV

A local desktop application for managing resumes, vacancies, and cover letters within projects.

## Developer documentation

- [Architecture and constraints](docs/architecture.md): current contracts and invariants.
- [Backlog and open questions](docs/backlog.md): deferred work, without a fixed implementation order.
- [Resume JSON Schema](docs/schemas/resume-content-v1.schema.json): the versioned document contract.
- [Vacancy content schema](docs/schemas/vacancy-content-v1.schema.json): the portable JSON export contract.
- [Vacancy recognition schema](docs/schemas/vacancy-recognition-v1.schema.json): the source-processing response contract.

## Project structure

This is a Kotlin Multiplatform project currently targeting Desktop (JVM).

- `desktopApp`: desktop entry point, window lifecycle, resources, and packaging.
- `domain`: application models, use cases, repository/provider contracts, and document serialization.
- `database`: Room entities, DAOs, local repositories, and database construction.
- `network`: Ktor transport, platform HTTP engines, and common timeout configuration.
- `providers`: external repositories, provider clients, response DTOs, and domain mapping.
- `shared/src/commonMain`: components, navigation, feature services, and Compose UI.
- `shared/src/jvmMain`: desktop dependency composition, filesystem, native pickers, and image processing.
- `shared/src/commonTest` and `shared/src/jvmTest`: fixtures, behavior tests, and screenshot tests.
- `database/schemas`: exported Room schemas.
- `gradle/libs.versions.toml`: dependency and plugin versions.
- `.run`: shared Android Studio run configurations.

## Run and package

Use the Gradle wrapper from the project root. In Android Studio, select **ReframeCV**
from `.run/ReframeCV.run.xml`, or run:

```shell
./gradlew :desktopApp:run
```

For hot reload:

```shell
./gradlew :desktopApp:hotRun --auto
```

Build a standalone application:

```shell
./gradlew :desktopApp:createDistributable
```

The output is in `desktopApp/build/compose/binaries/main/app/`. On macOS:

```shell
open desktopApp/build/compose/binaries/main/app/ReframeCV.app
```

## Tests and full checks

Select **FullCheck** in Android Studio (`.run/FullCheck.run.xml`) for shared JVM test
cleanup, automatic formatting, formatting checks, Detekt, all tests, coverage reports,
and coverage verification. It disables parallel execution and the build cache and
verifies existing goldens. `ktlintFormat` runs before the checks and modifies source
formatting; review the diff afterward.

Equivalent terminal command:

```shell
./gradlew cleanJvmTest ktlintFormat ktlintCheck detekt allTests :koverHtmlReport :koverXmlReport :koverVerify --no-parallel --no-build-cache
```

Run tests independently:

```shell
./gradlew allTests
./gradlew :shared:jvmTest
```

JVM tests include unit tests, database integration tests, and screenshot verification.
Gradle skips unchanged tasks. To clear shared JVM test outputs and rerun without the build cache:

```shell
./gradlew cleanJvmTest allTests --no-parallel --no-build-cache
```

The shared test report is `shared/build/reports/tests/jvmTest/index.html`.

## Screenshot references

Roborazzi verifies screenshots during ordinary JVM and IDE test runs. References live
in `shared/src/jvmTest/screenshots/`. Record changes only after an intentional UI update:

```shell
./gradlew :shared:recordRoborazziJvm --no-parallel
```

Review the image diff, then verify:

```shell
./gradlew :shared:verifyRoborazziJvm --no-parallel
```

Record and verify in the same OS/font/graphics environment. Compose Desktop support
in Roborazzi is experimental, and rendering can vary between environments.

## Formatting and static analysis

```shell
./gradlew ktlintCheck detekt --no-parallel
```

Apply canonical formatting separately, then review the diff:

```shell
./gradlew ktlintFormat --no-parallel
```

`.editorconfig` defines the `android_studio` style and a 100-character Kotlin line limit.
Generated sources are excluded. Run formatting before analysis rather than concurrently.

- ktlint reports: each module's `build/reports/ktlint/`.
- Detekt report: `build/reports/detekt/detekt.html`.
- Detekt configuration: `config/detekt/detekt.yml`; it covers Kotlin sources in both
  modules without type resolution, excluding generated/build output.

## Coverage

```shell
./gradlew allTests :koverHtmlReport :koverXmlReport :koverVerify --no-parallel
```

Kover combines `domain`, `database`, `network`, `providers`, `shared`, and `desktopApp`; the minimum is 75% line coverage.
Generated Room implementations and Compose resources are excluded.

- HTML: `build/reports/kover/html/index.html`.
- XML: `build/reports/kover/report.xml`.

## Local data and diagnostics

The application data directory contains `reframe-cv.db`, managed document files,
`stacktrace.txt`, and `openrouter-http.log`:

| Platform | Directory |
| --- | --- |
| macOS | `~/Library/Application Support/ReframeCV/` |
| Windows | `%APPDATA%/ReframeCV/` |
| Linux | `$XDG_DATA_HOME/ReframeCV/`, or `~/.local/share/ReframeCV/` when unset |

Managed vacancy sources live in `attachments/`; resume photographs and their draft
sources live in `documents/resume-photos/`. Back up the database and managed files together.
Deleting only the database does not remove these directories.

The development database stays at schema version 1, without migrations. After an
incompatible schema change, close the application before resetting the database.
A reset permanently removes its records; delete `reframe-cv.db` and any associated
`reframe-cv.db-wal` / `reframe-cv.db-shm` files only when those records can be discarded.
Migrations will be required when data must be preserved.

Open the data directory in Finder:

```shell
open "$HOME/Library/Application Support/ReframeCV"
```

Errors append timestamps, operation context, and stack traces to `stacktrace.txt`.
Each error includes the latest 200 in-memory Store/project-tree events. Summaries
omit document text and project names. The file survives restarts; event history does not.

OpenRouter connection checks and model discovery append HTTP requests and complete text
responses to `openrouter-http.log` and the console through Klogging (`OpenRouterHttp`, INFO),
including status, headers, and unknown JSON fields.
JSON is formatted for inspection. Authorization, cookies, secret JSON fields, and the
current API key are redacted. The file is created on the first request and survives restarts.

## License

[MIT](LICENSE.md). Dependency and asset notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
