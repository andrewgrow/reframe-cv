# ReframeCV

Look at your resume from a new angle.

ReframeCV is a local desktop application for managing your resumes within projects. Store, create, rewrite, and tailor resumes to job opportunities while keeping related versions together.

## Projects first

A project is the starting point for working with resumes. It is a flexible workspace whose purpose is defined by the user:

- A career direction, such as backend, full-stack, or mobile development.
- A company, with resumes tailored specifically to its opportunities.
- Any other grouping that fits the user's workflow.

Within a project, users will be able to keep source resumes, create and rewrite content, and organize adaptations for different vacancies. Projects are not restricted to a fixed category or tied exclusively to a role, company, or vacancy. More detailed project organization will be explored as the product develops.

## Planned workflow

1. Create or open a project for a chosen goal.
2. Store an existing resume or create a new one within that project.
3. Edit, rewrite, and review resume content.
4. Tailor resume versions to specific vacancies.
5. Keep related resumes and their versions organized within the project.
6. Export a resume as a PDF.

The current application provides a project list, project creation and editing, and local storage. Storing resume documents within projects, resume-specific editing, tailoring, version management, and PDF export are planned.

## Architecture

The application is built with Kotlin/JVM and Compose Multiplatform Desktop. Kotlin owns the desktop UI, application logic, and local project storage. Projects form the foundation for resume storage, creation, rewriting, and adaptation as those features are developed.

## Technologies

The application is local and single-user, with no backend server or web frontend. Project data is stored locally.

- Kotlin/JVM and Compose Multiplatform Desktop for the application and UI.
- [Decompose](https://arkivanov.github.io/Decompose/) for navigation and lifecycle management.
- [Room](https://developer.android.com/kotlin/multiplatform/room) with bundled SQLite for project data.
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) for date and time handling.
- Klogging for logging.
- [MVIKotlin](https://arkivanov.github.io/MVIKotlin/) for MVI state management.
- [Roborazzi](https://github.com/takahirom/roborazzi) for screenshot testing.
- [Detekt](https://detekt.dev/) for static code analysis.
- [ktlint](https://ktlint.github.io/) for Kotlin formatting checks and automatic formatting.

## Initial milestone

Development starts with projects: listing, creating, editing, and persisting the user's workspaces. The next milestone is to manage resumes within a project:

```text
create or open project -> add or create resume -> edit and save within project
```

From that foundation, the application can grow to support rewriting, vacancy-specific adaptations, version management, PDF export, templates, and import tools. The exact organization of projects remains open and will follow users' needs.

## Project structure

This is a Kotlin Multiplatform project targeting Desktop (JVM).

- `desktopApp` contains the desktop application entry point and packaging configuration.
- `shared/src/commonMain` contains shared UI and application code.
- `shared/src/jvmMain` contains JVM-specific implementations.

## Running the application

In Android Studio, select the shared **ReframeCV** run configuration in the toolbar and click Run. The configuration is stored in `.run/ReframeCV.run.xml` and runs `:desktopApp:run`. You can also use one of these commands:

- Hot reload: `./gradlew :desktopApp:hotRun --auto`
- Standard run: `./gradlew :desktopApp:run`

To build a standalone application:

```shell
./gradlew :desktopApp:createDistributable
```

The generated application is placed in `desktopApp/build/compose/binaries/main/app/`.

On macOS, launch it with:

```shell
open desktopApp/build/compose/binaries/main/app/ReframeCV.app
```

## Local storage

ReframeCV stores its database as `reframe-cv.db` in its own application data directory:

- macOS: `~/Library/Application Support/ReframeCV/`.
- Windows: `%APPDATA%/ReframeCV/`.
- Linux: `$XDG_DATA_HOME/ReframeCV/`, or `~/.local/share/ReframeCV/` when unset.

Existing ReframeCV data is not migrated automatically.

## Running tests

For a complete local run from Android Studio, select **ReframeCV - Full Check** in the toolbar. It clears JVM test outputs, applies `ktlintFormat`, checks formatting and Detekt, runs all tests including screenshot verification, generates HTML/XML coverage reports, and verifies the coverage threshold. It runs without parallel task execution or the build cache. This configuration modifies source formatting; review the diff afterward. Screenshot references are verified, not overwritten.


Use the run button in your IDE's editor gutter, or run all test suites at once from the project root:

```shell
./gradlew allTests
```

This currently runs the shared JVM unit and database integration tests together with Roborazzi screenshot verification. To run only the shared desktop tests, use:

```shell
./gradlew :shared:jvmTest
```

Gradle skips tests when nothing has changed since the last run. To force a full re-run (for example, to verify a clean state before pushing, or when test results look stale or flaky), clean the test outputs and disable the build cache:

```shell
./gradlew cleanJvmTest allTests --no-build-cache
```

Desktop screenshot tests use [Roborazzi](https://github.com/takahirom/roborazzi). Ordinary `jvmTest` and IDE test runs verify screenshots by default. To update reference images after an intentional UI change:

```shell
./gradlew :shared:recordRoborazziJvm
```

Review the updated images before committing them. To verify screenshots explicitly:

```shell
./gradlew :shared:verifyRoborazziJvm
```

Reference images are stored in `shared/src/jvmTest/screenshots/`. Roborazzi's Compose Desktop support is experimental, and screenshots can vary across operating systems, fonts, and graphics environments. Record and verify reference images in the same environment.

## Static analysis

Run Detekt locally from the project root:

```shell
./gradlew detekt
```

The HTML report is at `build/reports/detekt/detekt.html`. Analysis covers Kotlin application and test sources in both modules, excluding generated code. Default rules are enabled with adjustments for Compose naming, test fixtures, and design tokens. This initial setup runs without type resolution, so rules requiring type information are not evaluated.

Detekt is pinned to `2.0.0-alpha.6` to match the project's Kotlin version; this is a pre-release version.

## Code formatting

Check Kotlin sources, tests, and Gradle Kotlin scripts in all modules:

```shell
./gradlew ktlintCheck
```

Apply automatic formatting, then review the diff before committing:

```shell
./gradlew ktlintFormat
```

Formatting uses the `android_studio` style configured in `.editorconfig`, with a 100-character line limit and support for Compose function names. Enable EditorConfig support in your IDE to share applicable formatting settings. The IDE formatter and ktlint can still differ; use `ktlintFormat` for the canonical project formatting. Generated sources are excluded. Reports are written under each project's `build/reports/ktlint/` directory. Formatting runs only when explicitly requested; checks do not modify files.

To run formatting checks, static analysis, all tests, and the coverage check together:

```shell
./gradlew ktlintCheck detekt allTests :koverVerify
```

## Local code coverage

[Kover](https://github.com/Kotlin/kotlinx-kover) measures JVM code coverage across `shared` and `desktopApp`. To run tests, generate the combined HTML and XML reports, and check the minimum coverage:

```shell
./gradlew allTests :koverHtmlReport :koverXmlReport :koverVerify
```

Open `build/reports/kover/html/index.html` for the HTML report. The XML report is at `build/reports/kover/report.xml`. The initial minimum is 75% line coverage. Generated Room implementations and Compose resources are excluded; application code, including the desktop entry point, remains included. Add behavioral tests alongside new application logic.

## Help and tricks

Running opencode: Open a terminal in the project root and run:

```shell
opencode
```

Open the database folder in Finder on MacOS and looking up to `reframe-cv.db`:

```shell
open "$HOME/Library/Application Support/ReframeCV"
```

## License

ReframeCV is available under the [MIT License](LICENSE.md). See [Third-Party Notices](THIRD_PARTY_NOTICES.md) for dependency licenses.
