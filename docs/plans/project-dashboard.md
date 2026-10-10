# Plan: workspace models and project dashboard

## Working with this plan

- Complete the stages in order and check off items after verification.
- Before resuming work, read this file and inspect the current code.
- Keep agreed decisions, remaining questions, and verification results here.
- Do not treat open questions as agreed requirements; resolve them before dependent work.

## Goal

Clicking Configure opens the selected project's dashboard.
The dashboard contains three columns: **Resumes**, **Vacancies**, and **Cover letters**,
each showing the number of active records in its section.

Project navigation is part of this milestone: replace the left menu buttons
with a project tree and remove right-pane breadcrumbs and repeated project titles.
The right pane contains the main content area and a bottom controls area.

All three sections are independent. Users can start with vacancies or letters
without creating a resume. Relationships between records are optional.

## Agreed decisions

- Each record belongs to a project through `projectId`.
- Local identifiers are `Long`, as in the existing project model.
- Timestamps are `Long` values in milliseconds since the Unix epoch; use the
  existing `nowMillis()` helper to obtain the current time.
- All three models have `keywords: List<String>`, defaulting to an empty list.
- Records are soft-deleted through `deletedAt`; lists and counts exclude deleted records.
- Deleting a project marks the project and its descendants as deleted, preserving
  earlier deletion timestamps. Its resumes, vacancies, letters, keywords, and
  relationships remain unchanged.
- Lists, searches, and counts exclude records whose owning project is deleted;
  record creation, updates, and relationship selection reject deleted projects.
- Project restoration is outside this milestone. Future branch restoration must
  distinguish descendants deleted with the parent from those deleted earlier.
- Resumes and letters initially store editable text.
- A project may select one main resume through `Project.mainResumeId: Long?`.
- Adaptations default to the main resume as their source; users may choose another.
- Assign only an active resume belonging to the same project; clear the selection
  atomically when that resume is soft-deleted.
- Opening the dashboard does not change the project's mode.
- Creating the first record in any section changes the project to `Workspace`.
- A project in `Container` mode cannot contain its own workspace records.
- A project in `Workspace` mode cannot contain subprojects.
- Record creation and the mode transition must occur in a single transaction.
- Soft-deleting a resume clears the project's main selection, adaptation source
  references, and vacancy resume references in the same transaction.
- Soft-deleting a letter clears vacancy letter references in the same transaction.
- Clearing references preserves the independent text of adaptations and vacancies.
- Deleting the last active record across all three sections returns the project
  to `Unconfigured` in the same transaction.

## Navigation and screen layout

- The left pane shows a vertically scrollable project tree rooted at `Projects`.
- Tree entries are text rows with indentation for nesting, rather than menu buttons.
- Clicking an expansion arrow reveals or hides children; clicking a project name
  opens that project in the right pane. These are separate actions.
- Clicking `Projects` opens the root project list.
- Successful project creation closes the editor and immediately opens the new project,
  including subprojects. The parent list remains reachable through the tree or Back.
- Renaming does not navigate; failed creation keeps the editor open.
- Highlight the current project with contrasting text and a subtle background;
  keep hover and keyboard focus visible.
- When a project is opened, expand its ancestors and scroll the selected row into view.
- Truncate long names with an ellipsis and show the full name on hover.
- Remove breadcrumb rows and repeated project titles from the right pane.
  The selected project in the left tree identifies the current destination.
- Align menu and content rows using the same token-based top spacing.
- The main content area uses the available space and scrolls independently;
  controls remain anchored at the bottom.
- Keep the child-project list and its Edit actions in the right pane;
  the tree provides navigation rather than replacing project management.
- Navigation remains owned by the root component and uses project identifiers;
  removing visual breadcrumbs does not remove the underlying hierarchy.

## Models

Fields shared by `Resume`, `Vacancy`, and `CoverLetter`:

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long` | Local identifier |
| `projectId` | `Long` | Owning project |
| `name` | `String` | Record name shown in lists |
| `keywords` | `List<String>` | Keywords for searching |
| `createdAt` | `Long` | Creation timestamp |
| `updatedAt` | `Long` | Last modification timestamp |
| `deletedAt` | `Long?` | Soft-deletion timestamp; `null` for an active record |

### Resume

| Field | Type | Purpose |
| --- | --- | --- |
| `content` | `String` | Resume text |
| `sourceResumeId` | `Long?` | Source resume, if this record was created as an adaptation |

An adaptation is an independent editable record. Editing its text does not
change the source resume or other adaptations. `sourceResumeId` records its
origin; changes are not inherited automatically.

### Vacancy

| Field | Type | Purpose |
| --- | --- | --- |
| `description` | `String` | Vacancy text |
| `company` | `String` | Company name; may be empty |
| `url` | `String` | Vacancy URL; may be empty |
| `resumeId` | `Long?` | Selected resume |
| `coverLetterId` | `Long?` | Selected cover letter |

A vacancy has at most one selected resume and one selected letter.
The same resume or letter may be used for multiple vacancies.
Do not store lists of vacancy IDs in resumes or letters:
query the reverse relationships when needed.

### CoverLetter

| Field | Type | Purpose |
| --- | --- | --- |
| `content` | `String` | Cover letter text |

A letter can be created and stored without a relationship to a vacancy or resume.

## Keywords

Domain models represent keywords as lists of strings.
For indexed searches by individual keywords, use the database tables
`ResumeKeyword`, `VacancyKeyword`, and `CoverLetterKeyword`.

- Each table row represents one keyword belonging to one record.
- Each table has a foreign key to its corresponding entity and an index on
  the normalized keyword.
- Trim surrounding whitespace and remove empty values and duplicates when saving.
- Searches are case-insensitive: `Kotlin` and `kotlin` match the same keyword.
- Update a record and its keywords atomically.
- Keyword searches exclude soft-deleted records.

## Stage 1. Domain models

- [x] Add `Resume`, `Vacancy`, and `CoverLetter` in separate model packages.
- [x] Add the shared and model-specific fields listed above.
- [x] Define defaults for optional fields and `keywords`.
- [x] Follow the existing `DomainModel` contract and project conventions.

## Stage 2. Database and DAOs

- [x] Add the main-resume foreign key when the resume table is introduced.
- [x] Add Room entities and mappings between database entities and domain models.
- [x] Add keyword tables and the required foreign keys.
- [x] Add indexes on `projectId`, relationship foreign keys, and keywords
  based on the actual queries.
- [x] Add DAOs for creating, reading, updating, and soft-deleting records.
- [x] Add observation of active records and their counts in the selected project.
- [x] Add keyword searches for each entity.
- [x] Implement transactional record creation that changes the project to `Workspace`.
- [x] Reject record creation in a missing, deleted, or container project.
- [x] Preserve the rule that `Workspace` projects cannot contain subprojects.
- [x] Register the new entities and DAOs in `AppDatabase`.
- [x] Regenerate the single version 1 schema and recreate the old local database
  if necessary. There is no real user data yet, so migrations are not required.

## Stage 3. Repositories and dependencies

- [x] Add separate `ResumesRepository`, `VacanciesRepository`,
  and `CoverLettersRepository` contracts.
- [x] Add local implementations that encapsulate DAOs and transactions.
- [x] Provide the repositories through `ApplicationDependencies`.
- [x] Add observation of the three section counts for the dashboard.
- [x] Provide observation of active projects for the navigation tree, including
  hierarchy and name changes.
- [x] Validate that related records belong to the selected project.
- [x] Reject relationships to missing or soft-deleted records.
- [x] Validate main-resume selection and clear it atomically on resume deletion.
- [x] Use the existing time, error-handling, and lifecycle mechanisms.

## Stage 4. Project tree and dashboard navigation

- [x] Replace the root's left menu buttons with a project-tree component.
- [x] Model tree expansion and current selection independently from right-pane content.
- [x] Observe active projects and update the tree after creation, renaming, and deletion.
- [x] Wire tree-name clicks and the `Projects` root to existing Decompose navigation.
- [x] Expand ancestors and reveal the selected row when navigation originates
  from the right pane or Back.
- [x] Preserve expansion and scroll state when switching right-pane screens.
- [x] Define a consistent destination when the currently open project is deleted.
- [x] Add a dashboard component with the project identifier; show its name in the left tree.
- [x] Pass the Configure callback from the component to `EmptyProjectActions`.
- [x] Add the Configure transition through the existing Decompose navigation.
- [x] Preserve hierarchy-aware navigation and Back without visual breadcrumb rows.
- [x] Open the dashboard when revisiting a project in `Workspace` mode.
- [x] Preserve the current subproject list for `Container` mode.
- [x] Preserve the Configure / Add project choice for `Unconfigured` mode.
- [x] Observe counts and cancel observation when the component is destroyed.
- [x] Distinguish loading, ready, and error states.
- [x] Use the existing delayed loading indicator so zero counts are not shown
  as results before the initial read completes.

## Stage 5. Dashboard layout

- [x] Add the scrollable text-based project tree with expansion arrows,
  selection, hover, keyboard focus, and full-name tooltips.
- [x] Remove right-pane breadcrumb rows and repeated project titles.
- [x] Keep project names current in the left tree after renaming.
- [x] Keep the main content scrollable and the bottom controls anchored.
- [x] Create a separate dashboard UI package alongside the other project screens.
- [x] Put the screen container and reusable section block in separate files.
- [x] Show three columns: Resumes, Vacancies, and Cover letters.
- [x] Show actual active-record counts; empty sections display `0`.
- [x] Use English string resources and the tokens and colors from `ReframeTheme`.
- [x] Use `SelectableText` for ordinary text where it does not interfere with interaction.
- [x] Support narrow windows and increased UI scale without clipping content.
- [ ] Connect creation actions when their corresponding editors are implemented.
  The vacancy list's Add button is deliberately a placeholder while list behavior
  is being refined, as agreed on 2026-10-10.

## Stage 6. Verification

- [x] Verify persistence and reading of all three models, including optional relationships.
- [x] Verify independent creation of vacancies and letters without a resume.
- [x] Verify reuse of one resume and letter across multiple vacancies.
- [x] Verify keyword normalization, deduplication, and case-insensitive searches.
- [x] Verify that lists, searches, and counts exclude soft-deleted records.
- [x] Verify transactional mode changes: a failed write must not leave the project in `Workspace`.
- [x] Verify that subprojects and workspace records cannot coexist in a project.
- [x] Verify Configure and Back without changing project mode, including deletion of the open dashboard project.
- [x] Verify reopening a project in `Workspace` mode once mode-based routing is implemented.
- [x] Verify tree expansion separately from opening a project, root navigation,
  current selection, ancestor expansion, and scrolling the selection into view.
- [x] Verify tree updates after creation, renaming, and deletion, including
  deletion of the currently open project.
- [x] Verify keyboard navigation and focus visibility in the tree.
- [x] Verify tree name updates and independent tree / content scrolling.
- [x] Add isolated tree goldens covering nesting, expansion, selection,
  long names, and both themes.
- [x] Update full-screen goldens for the new left navigation and content layout;
  remove obsolete breadcrumb-specific tests and references once replaced.
- [x] Verify count updates and dashboard loading / error states.
- [x] Add dashboard goldens in dark and light themes, with zero and nonzero counts.
- [x] Add a separate golden for the reusable section block if its states need
  detailed coverage; do not duplicate all of them on the full screen.
- [x] Verify narrow windows and increased UI scale.
- [x] Run formatting, ktlint, Detekt, all tests, and coverage verification
  using the current README instructions; visually review new goldens.

## Stage 7. Vacancy list and editor

- [x] Start the section UI with vacancies rather than resumes.
- [x] Make the entire vacancy section on the dashboard open the vacancy list.
- [x] Open the selected project's vacancy list in the right pane, preserving tree selection.
- [x] Display creation date, company, and title; use createdAt rather than updatedAt.
- [x] Show dates as YYYY-MM-DD in the device's local time zone.
- [x] Use a table in wider panes and stacked records in narrow panes or at larger UI scales.
- [x] Observe active vacancies through VacanciesRepository with delayed loading, empty, and error states.
- [x] Show Retry only on load failure, between Add and Back in the bottom controls.
- [x] Cancel observation when the component is destroyed.
- [x] Keep Back anchored at the bottom and return to the dashboard through existing navigation.
- [x] Remove the vacancy screen from history when its project is deleted.
- [x] Cover navigation, loading, live updates, cancellation, scrolling, date boundaries,
  and dark / light / narrow / increased-scale screenshots.
- [x] Add a visible Add button beside Back, with no action for now.
- [ ] Finish refining list behavior before starting the vacancy editor.
- [ ] Agree on the vacancy editor, Save / Cancel behavior, and minimum draft validation.
- [ ] Add the vacancy creation action and editor, then connect updating existing vacancies.

## Open questions

- [ ] Define minimum name and content validation when creating records.
- [x] Define relationship behavior when a resume, letter, or vacancy is soft-deleted.
- [x] Define workspace-record behavior when their project is deleted.
- [x] Decide whether a project stays in `Workspace` after its last record is deleted.
- [ ] Define searches with multiple keywords: match all or any.
- [x] Agree on the dashboard layout for narrow windows.
- [ ] Agree on left-pane sizing and how deep nesting behaves in narrow windows.
- [ ] Decide how Back behaves when switching between unrelated tree branches
  and when returning from an unconfigured project's dashboard.
- [ ] Before implementing section screens, agree on their navigation, editors,
  and the workflow for creating a resume adaptation.

## Current state

The three domain models are implemented with the agreed fields and optional defaults.
The project model and version 1 schema include nullable `mainResumeId` / `main_resume_id`.
The database now contains projects, resumes, vacancies, cover letters, and three
keyword tables, with foreign keys and indexes. Complete record reads include keywords
in their saved order; writes normalize whitespace and deduplicate case-insensitively,
while preserving the first spelling for display. Single-keyword searches are indexed.
DAOs provide transactional creation, updates, soft deletion, active lists, and counts.
Creation promotes a project to Workspace atomically and rejects missing, deleted,
or Container projects. Relationship writes require active records in the same project.
Main-resume selection and deletion cleanup are implemented; deleting the final active
workspace record returns the project to Unconfigured.
Lists, counts, and searches exclude records owned by deleted projects. Project deletion
preserves its workspace records and relationships, including their existing deletion
states; only projects and their descendants are marked deleted. Restoration is not
implemented in this milestone.
Separate repositories now encapsulate transactional DAO writes, active record
observation, and keyword searches. ApplicationDependencies lazily shares one database
across all four repositories and prevents service access after closing.
Record creation and updates use nowMillis(); minimum name / content validation
remains an open question for the editors.
Configure opens the dashboard in the existing Decompose stack,
with three full-height section columns and no selection or creation actions.
It preserves the tree selection and does not change the project's mode.
Back returns to the project's empty screen; deleting the project removes its dashboard
from history and returns to the nearest surviving project or the root.
Dashboard columns use dedicated theme background colors and token-based spacing.
They share the available width and retain a minimum width of 160 dp at default scale;
narrow windows use horizontal scrolling with fixed outer padding.
The dashboard observes all three active-record lists for its project. Counts are
computed from these same lists rather than separate count queries, so each section's
count matches its displayed records. Each column scrolls vertically independently.
Narrow dashboards scroll horizontally; long record names wrap and remain selectable.
Loading persists until all three lists have emitted; the spinner appears only after
one second. Failed reads expose Retry, which replaces the previous observation.
Destroying the component disposes its store and cancels all observations.
Reopening Workspace projects routes to the dashboard; Container and Unconfigured
projects retain their project-list and empty-project screens.
The project already has `ProjectMode` values `Unconfigured`, `Container`,
and `Workspace`; Configure opens the dashboard without persisting a mode change.
The root now displays a project tree with separate expansion and navigation actions,
selection, hover, keyboard focus, long-name tooltips, and automatic scrolling to selection.
Active projects are observed across all levels; creation, renaming, and deletion update the tree.
Deleting the open project returns to its nearest surviving ancestor or the root list,
and deleted destinations are removed from navigation history.
The left pane uses the existing navigation-width token. Deep nesting adds horizontal
scrolling while ordinary branches retain their left inset. Expansion and scroll state
remain owned by the root navigation UI when the right-pane screen changes.
Tree navigation uses the existing Decompose stack: opening a new destination adds
it to history, while selecting a destination already in the stack returns to it.

Project names are shown in the left tree without a duplicate right-pane title.
The tree and project list use the same medium top spacing and aligned row text.
The visual breadcrumb component and title component, their tests, and goldens
have been removed; the underlying project path is retained for hierarchy navigation.
Main content scrolls independently, while empty-screen and list controls stay at the bottom.
Opening help no longer moves the controls, including on narrow project screens.

The first section screen is now the vacancy list. Clicking its entire dashboard section
opens the selected project's active vacancies without changing its mode. Rows show
creation date, company, and title; missing companies have an explicit placeholder.
Dates use the device's local time zone and ISO calendar-date format. Table widths
and the switch to stacked records use scalable UI tokens. The list scrolls while
Back remains anchored at the bottom. Retry replaces the active observation.

Next: refine the vacancy list. Add is visible next to Back and intentionally does
nothing for now; the creation screen is deferred by the user. After list behavior
is settled, agree on the editor and minimum draft validation, then implement
vacancy creation and editing. Resume and cover-letter section screens follow later.

Verification on 2026-10-05: all 158 JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed with:

```shell
./gradlew cleanJvmTest ktlintCheck detekt allTests :koverVerify --no-parallel --no-build-cache
```

Project-screen, root-screen, loading, empty-project, and UI-scale goldens were updated.
Isolated tree and project-list goldens were visually reviewed in dark and light themes,
including the expanded project help on a narrow screen.

Database verification includes round-trip mappings, reopening persisted records,
optional and reused links, foreign-key rejection, atomic rollback, invalid updates,
keyword replacement and observation, section-count observation, deletion cleanup,
and competing child / workspace creation. Generated Room DAO implementations are
excluded from coverage using the same rule previously applied to ProjectDao.
The local development database must be recreated before launching against this schema.

Dashboard verification includes dark / light full-screen goldens, a narrow layout,
and 150% scale. Functional tests cover Configure, Back, unchanged project mode,
project deletion while viewing the dashboard, and scrolling to all three sections.

## Follow-up after this milestone: user-accessible Back navigation

- [ ] Discuss keyboard shortcuts and hardware Back input on supported platforms.
- [ ] Discuss an on-screen Back button, possibly at the top; placement is undecided.
- [ ] Once agreed, connect both input paths to the existing navigation history and
  define their behavior at the root and while a dialog is open.

The vacancy list has a contextual Back button returning to its dashboard.
Global Back navigation is available programmatically; desktop keyboard input and
an application-wide Back button are not connected yet.

Repository and dashboard verification added on 2026-10-06 covers active-project
scoping, keyword mappings, write timestamps, updates, soft deletion, dependency reuse,
combined initial reads, live updates, Retry, and cancellation on component destruction.
An integration test observes all three sections through ApplicationDependencies and
checks record and project soft deletion against the real database.
Dashboard goldens cover empty and populated columns in both themes, a failed read,
narrow windows, 150% scale, and isolated populated / empty sections.

Verification on 2026-10-06: all 175 JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed using the full command above.
New and updated dashboard goldens were visually reviewed.

Verification on 2026-10-10: all 188 JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed using the full command above. Vacancy-list
and updated dashboard goldens were visually reviewed in both themes, narrow panes,
and at increased UI scale. Vacancy creation and editing remain the next step.
