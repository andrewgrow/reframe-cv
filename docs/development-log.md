# Development log: workspace models and project dashboard

## Status

Archived on 2026-10-12. The original milestone—workspace models, local persistence,
project-tree navigation, and the dashboard—is complete. Vacancy management, source
file attachments, Back navigation, and permanent deletion were added during this work.

This file records development history and the reasons behind decisions. It is not
an active implementation plan. Some passages describe earlier behavior that was
later replaced; current code and active plans take priority.

Unfinished work and open questions have moved to
[Workspace editors and vacancy sources](plans/workspace-editors.md).
Completed checkboxes below describe what was verified at the time. Deferred entries
preserve the old scope without claiming that the work was completed.

## Historical scope and decisions

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
- Projects, resumes, cover letters, and vacancies are permanently deleted.
  Models and tables have no `deletedAt` / `deleted_at` fields.
- Deleting a project cascades through all descendants, workspace records, and keywords.
- Deleting a vacancy or project also deletes affected snapshots after their last
  vacancy reference. Shared snapshots referenced outside the deleted branch remain.
- Project deletion and unused-source metadata cleanup form one database transaction.
  File deletion follows commit under the same snapshot write/cleanup mutex.
  Failed file deletion is logged and retried through orphan cleanup on restart.
- Record creation, updates, and relationship selection reject missing projects/records.
- Deleting a container's last child returns the parent to `Unconfigured`.
- Resumes and letters initially store editable text.
- A project may select one main resume through `Project.mainResumeId: Long?`.
- Adaptations default to the main resume as their source; users may choose another.
- Assign only an active resume belonging to the same project; clear the selection
  atomically when that resume is deleted.
- Opening the dashboard does not change the project's mode.
- Creating the first record in any section changes the project to `Workspace`.
- A project in `Container` mode cannot contain its own workspace records.
- A project in `Workspace` mode cannot contain subprojects.
- Record creation and the mode transition must occur in a single transaction.
- Deleting a resume clears the project's main selection, adaptation source
  references, and vacancy resume references in the same transaction.
- Deleting a letter clears vacancy letter references in the same transaction.
- Clearing references preserves the independent text of adaptations and vacancies.
- Deleting the last active record across all three sections returns the project
  to `Unconfigured` in the same transaction.

## Imported source records

- [x] Add `ImportRecord` and the `import_records` table for one immutable raw
  source item per snapshot, independent of projects and workspace records.
- [x] Store `provider`, `rawData`, `createdAt`, optional `externalId`, `sourceUrl`,
  and `metadataJson`. No `updatedAt`: fetching again creates a new snapshot.
  No `deletedAt`: unused snapshots are physically deleted.
- [x] Add optional `Vacancy.importRecordId` with an indexed foreign key.
- [x] Allow repeated provider/external ID pairs and sharing a snapshot between vacancies.
- [x] Preserve snapshots during ordinary vacancy edits. Vacancy and project deletion
  remove affected snapshots only after the last reference.
- [x] Allow physical deletion of unreferenced snapshots; preserve snapshots still
  referenced by remaining vacancies.
- [x] Verify persistence, mapping, optional fields, deletion, and foreign-key rollback.
- Keep schema version 1 while no real user data needs migration.

### Source snapshot attachment

- [x] Indicate future snapshot attachment in the vacancy creation and editing dialogs with a
  disabled button and a short explanation.
- The initial placeholder has been replaced by local file attachment. URL fetching
  and parsing remain separate future work.
- [x] Support JSON, HTML, TXT, PNG, JPG/JPEG, PDF, and DOCX as original snapshots.
- [x] Store original bytes, format, and filename without changing the source content.
- [x] Commit snapshot metadata and vacancy in one database transaction after copying
  the file; remove the copy on rollback. Cancel never persists a selection.
- [x] Store UUID file copies under `attachments/` beside the database, with relative
  paths, original names, media types, byte sizes, and SHA-256 hashes in ImportRecord.
  File snapshots have nullable `rawData`.
- [x] Open, replace, and detach snapshots in both creation and editing dialogs.
  Retain metadata for missing files and warn when stored bytes have changed.
- [x] Remove unused files after replacement or vacancy deletion. Preserve shared
  snapshots referenced outside the deleted branch. Serialize orphan
  cleanup with writes; log failed cleanup. Failed startup cleanup retries on the
  next observation; leftover files from a failed deletion are removed on the next start.
- Deferred: Add URL fetching separately from parsing; vacancy fields remain independently editable.

Import workflows and repositories will be added when scraper integration begins.

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

No model has a deletion timestamp: all deletions are permanent.

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
| `importRecordId` | `Long?` | Immutable imported source snapshot |

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
- Keyword searches exclude deleted records.

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
- [x] Add DAOs for creating, reading, updating, and deleting records according to their retention policy.
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
- [x] Reject relationships to missing or deleted records.
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
- Deferred: Connect creation actions when their corresponding editors are implemented.
  Vacancy creation is connected through the list's Add dialog; other sections follow later.

## Stage 6. Verification

- [x] Verify persistence and reading of all three models, including optional relationships.
- [x] Verify independent creation of vacancies and letters without a resume.
- [x] Verify reuse of one resume and letter across multiple vacancies.
- [x] Verify keyword normalization, deduplication, and case-insensitive searches.
- [x] Verify that lists, searches, and counts exclude deleted records.
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
- [x] Display Title, Company, and Added in that order, with a fixed-width date column;
  use createdAt rather than updatedAt.
- [x] Show dates as YYYY-MM-DD in the device's local time zone.
- [x] Use a table in wider panes and stacked records in narrow panes or at larger UI scales.
- [x] Observe active vacancies through VacanciesRepository with delayed loading, empty, and error states.
- [x] Show Retry only on load failure, beside Add in the bottom controls.
- [x] Cancel observation when the component is destroyed.
- [x] Show Back in the vacancy page header and return to the dashboard.
- [x] Remove the vacancy screen from history when its project is deleted.
- [x] Cover navigation, loading, live updates, cancellation, scrolling, date boundaries,
  and dark / light / narrow / increased-scale screenshots.
- [x] Add a visible Add button in the bottom controls that opens the vacancy creation dialog.
- [x] Add a creation dialog with Title, Company, Description, URL, and comma-separated Keywords.
- [x] Allow resizing the vacancy dialog horizontally and vertically with a bottom-right handle.
  Preserve the initial rendered size as its minimum, keep the dialog centered, constrain it
  to the application window, and retain the chosen size until the dialog closes.
- [x] Keep dialog actions at the bottom and disable resizing while saving or confirming deletion.
- [x] Allow vertical resizing of Description using a bottom-right drag handle.
  Keep a token-based minimum height, with no maximum; remember height while the dialog is open.
- [x] Automatically scroll the form while dragging at its visible top or bottom edge;
  keep Save and Cancel anchored and disable resizing while saving or confirming deletion.
- [x] Require only a nonblank Title; optional fields may be empty.
- [x] Save through the repository; keep the form open on failure and allow retry.
- [x] After selecting a source file, highlight a blank or whitespace-only Title,
  show a required-title hint, and reveal/focus the field. Clear the error when a
  title is entered; optional fields do not block saving.
- [x] Prevent duplicate writes and dismissal while saving; focus Title on opening.
- [x] Cover creation, cancellation, validation, failure/retry, and dialog screenshots.
- [x] Open the same dialog from a vacancy row with prefilled fields and Edit vacancy title.
- [x] Update existing records while preserving IDs, creation dates, and relationships.
- [x] Cover row clicks, edit cancellation, update failure/retry, and edit screenshots.
- [x] Add a critical-colored Delete action at the left edge of the edit dialog.
- [x] Require inline confirmation: Delete this vacancy permanently? / Yes, delete this.
- [x] Animate confirmation and Save visibility; Cancel/Back cancels confirmation first.
- [x] Delete through the repository, block competing actions, and preserve confirmation on failure.
- [x] Cover confirmation, cancellation, duplicate prevention, failure/retry, and deletion screenshots.

## Open questions

- Deferred: Define minimum name and content validation when creating records.
- [x] Define relationship behavior when a resume or letter is deleted or a vacancy is deleted.
- [x] Define workspace-record behavior when their project is deleted.
- [x] Decide whether a project stays in `Workspace` after its last record is deleted.
- Deferred: Define searches with multiple keywords: match all or any.
- [x] Agree on the dashboard layout for narrow windows.
- Deferred: Agree on left-pane sizing and how deep nesting behaves in narrow windows.
- [x] Define Back behavior across tree branches and project screens: project and
  dashboard Back opens the parent project list regardless of the entry route.
- Deferred: Before implementing section screens, agree on their navigation, editors,
  and the workflow for creating a resume adaptation.

## Recorded implementation state

The three domain models are implemented with the agreed fields and optional defaults.
The project model and version 1 schema include nullable `mainResumeId` / `main_resume_id`.
The database now contains projects, resumes, vacancies, cover letters, and three
keyword tables, with foreign keys and indexes. Complete record reads include keywords
in their saved order; writes normalize whitespace and deduplicate case-insensitively,
while preserving the first spelling for display. Single-keyword searches are indexed.
DAOs provide transactional creation, updates, deletion, active lists, and counts.
Resumes and letters use physical deletion; vacancies use physical deletion with unused-source cleanup.
Creation promotes a project to Workspace atomically and rejects missing or Container projects. Relationship writes require active records in the same project.
Main-resume selection and deletion cleanup are implemented; deleting the final active
workspace record returns the project to Unconfigured.
Project deletion physically removes its descendants and workspace records through foreign-key
cascades, including keyword rows. Optional links from surviving records are cleared.
Affected source snapshots are deleted only when no vacancy references them, and
unused file copies are removed after commit. No restoration flow is planned.
Separate repositories now encapsulate transactional DAO writes, active record
observation, and keyword searches. ApplicationDependencies lazily shares one database
across all four repositories and prevents service access after closing.
Record creation and updates use nowMillis(); minimum name / content validation
remains an open question for the editors.
Configure opens the dashboard in the existing Decompose stack,
with three full-height section columns and no selection or creation actions.
It preserves the tree selection and does not change the project's mode.
Back opens the parent project list; deleting the project removes its dashboard
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
title, company, and creation date, with the fixed-width date column last;
missing companies have an explicit placeholder.
Dates use the device's local time zone and ISO calendar-date format. Table widths
and the switch to stacked records use scalable UI tokens. The list scrolls while
Add and optional Retry remain anchored at the bottom. Back is in the page header.
Retry replaces the active observation.

Add now opens a dialog. Only Title is required; Keywords are comma-separated.
Saving creates a vacancy in the selected project and updates observable lists/counts.
Save errors preserve the form for retry; saving blocks duplicate submissions and closing.
Vacancy rows now open the same dialog for editing. Save updates only editable fields,
preserving identity, creation dates, and relationships. Resume and cover-letter
section screens follow later.

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

## User-accessible Back navigation — completed

Back navigation work is complete for the current milestone. Header buttons,
Escape, hardware Back, dialog priority, and parent-project returns are
implemented and verified; no Back-related work remains open in this plan.

- [x] Add a Back arrow beside the Vacancies heading; return to the project's dashboard.
- [x] Add a Dashboard heading with the same arrow; return to the parent project
  list, including after Configure and when opened directly from the tree.
  Top-level projects return to the root list.
- [x] Handle Escape on the active page, including when the left tree has focus.
- [x] Give the vacancy editor priority: Escape cancels deletion confirmation first,
  then closes the dialog; saving and deletion block closing. A later Escape returns
  from the page. Handle key-down only to avoid navigating on the dialog's key-up.
- [x] Connect hardware Back to the same dashboard return destination.
- [x] Remove Back from the vacancy footer; keep Add and optional Retry there.
- [x] Show the current project title and Back arrow on an opened project's screen,
  including configuration and subproject lists. The page callback, Escape, and
  hardware Back open the parent list; an open project editor retains dismissal priority.
  Keep the root project list without a Back arrow or Escape navigation action.

Parent-project navigation supersedes the earlier history-based return behavior
recorded below. Returning from Test 5 after opening it directly from the root tree
opens Test 4, then proceeds through ancestors to the root. Missing ancestor screens
are created in the stack, and unrelated branches are removed from the return path.
Vacancies still returns to its dashboard; dialogs retain Back / Escape priority.

Parent-project Back verification on 2026-10-11: all 248 tests across shared and
desktop modules, screenshot verification, ktlint, Detekt, and coverage verification
passed. Five-level regression tests cover opening Test 5 from the root tree,
returning to Test 4 from both configuration and a persisted workspace, page and
hardware callbacks, Configure, tree selection, and repeated returns to the root.

Project-screen Back verification on 2026-10-11: all 238 JVM tests, screenshot
verification, ktlint, Detekt, and coverage verification passed. Tests cover arrow
clicks, Escape with tree focus, returning from nested unconfigured projects,
hardware Back, and the absence of Back at the root. Configuration, subproject-list,
and loading goldens were updated; light, dark, and narrow help layouts were reviewed.

Dashboard Back regression fixed on 2026-10-11: return through navigation history
instead of synthesizing the workspace's configuration screen. A database-backed
test covers workspaces with saved vacancies opened from root and nested project
lists, checking both the page callback and hardware Back. All 234 JVM tests,
screenshot verification, ktlint, Detekt, and coverage verification passed.

Back navigation verification on 2026-10-11: all 233 JVM tests, screenshot
verification, ktlint, Detekt, and coverage verification passed using the full
command above. Tests cover header clicks, Escape with tree focus, dialog priority,
the saving guard, and returning from a workspace opened directly from the tree.
Dashboard and vacancy-list goldens were updated and visually reviewed in dark
and light themes, including narrow layouts and increased UI scale.

Repository and dashboard verification added on 2026-10-06 covers active-project
scoping, keyword mappings, write timestamps, updates, physical deletion, dependency reuse,
combined initial reads, live updates, Retry, and cancellation on component destruction.
An integration test observes all three sections through ApplicationDependencies and
checks record deletion and project physical deletion against the real database.
Dashboard goldens cover empty and populated columns in both themes, a failed read,
narrow windows, 150% scale, and isolated populated / empty sections.

Verification on 2026-10-06: all 175 JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed using the full command above.
New and updated dashboard goldens were visually reviewed.

Verification on 2026-10-10: all 188 JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed using the full command above. Vacancy-list
and updated dashboard goldens were visually reviewed in both themes, narrow panes,
and at increased UI scale. Vacancy creation and editing are implemented through a shared dialog.

Snapshot placeholder verification on 2026-10-10: all 221 JVM tests, screenshot
verification, ktlint, Detekt, and coverage verification passed. Creation goldens
were updated and reviewed; the source attachment is disabled and performs no work.
The database schema and snapshot persistence remain unchanged.

Description resizing verification on 2026-10-11: all 225 JVM tests, screenshot
verification, ktlint, Detekt, and coverage verification passed. Mouse tests cover
growth, shrinking to the minimum, preserved text and height during editing,
automatic scrolling with growth beyond an 800x600 viewport, and disabled resizing
during deletion confirmation. Dialog goldens were updated and visually reviewed
in both themes, including an expanded description.

Dialog resizing verification on 2026-10-11: all 230 JVM tests, screenshot
verification, ktlint, Detekt, and coverage verification passed. Tests cover both
axes, the initial minimum, window bounds, immediate reverse dragging at the limit,
preserved dimensions during editing, resizing after description growth, and disabled
handles during saving or deletion confirmation. Default and expanded dialog goldens
were visually reviewed. Description and dialog handles share the same resize component.


File attachment verification on 2026-10-11: all JVM tests, screenshot verification,
ktlint, Detekt, coverage verification, and macOS packaging passed. Added tests cover
original-byte copying, missing/changed files, rollback, replacement, shared snapshots,
detachment, project physical deletion, serialized cleanup, failed file deletion, selection
cancellation, and pending editor changes. Isolated snapshot goldens and updated
vacancy-dialog goldens were reviewed in dark and light themes.


## Permanent deletion decision

Permanent deletion supersedes the earlier retention behavior recorded in verification
history. Projects, resumes, and cover letters no longer retain deleted rows. Deleting
a project removes its entire branch and workspace contents. Resume and letter deletion
clears optional relationships while preserving surviving documents and vacancy text.
Shared source snapshots are retained until their final vacancy reference is removed.
The development schema remains version 1 and requires recreating the local database.

Permanent deletion verification on 2026-10-11: all 283 JVM tests, screenshot
verification, ktlint, Detekt, coverage verification, and macOS packaging passed.
Tests cover recursive branch removal, document relationship cleanup, missing/deleted
record rejection, shared and exclusive snapshot cleanup, persistence after reopening,
and returning a parent with no remaining children to Unconfigured.


## Milestone closure — 2026-10-12

The original dashboard milestone is complete. Resume and cover-letter domain models,
DAOs, repositories, and dashboard observation exist, but their section screens and
editors do not. Main-resume selection exists in the model and DAO, without a user-facing
selection flow. Resume adaptation, URL fetching, and source parsing are future work.

Gradle Wrapper was upgraded from 9.7.0 to 9.8.1. All 286 tests, screenshot verification,
ktlint, Detekt, coverage reports and verification, and macOS packaging passed.
The new required-title feedback after selecting a vacancy attachment is also implemented.


## Vacancy editor page — 2026-10-12

Vacancy creation and editing now open as full pages in the project pane, leaving
the project tree available. A nested list/editor navigation stack preserves the
list position, and the editor component owns the draft for future processing tools.
The header provides Back; Escape and hardware Back use the same behavior.
Confirmation is cancelled before leaving, and navigation is blocked during writes.

The form scrolls independently of the fixed action footer. Source attachments,
required-title feedback, save/delete failures, and description resizing are retained.
The former dialog resize controls and their tests were removed.

Verification: all 286 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports and verification, and macOS packaging passed. Updated editor references
and new application-shell references were visually reviewed in dark/light themes
and at a narrow window width. Navigation tests cover draft ownership, return to
the list, hardware Back, and blocking tree navigation during saving/deletion.


## Reusable resizable dialog — 2026-10-12

Dialog resizing is retained as a shared ResizableDialog and DialogResizeState in
ui.compose.common, now used by project creation and editing. Dragging the lower-right
handle changes both dimensions, bounded by the initial minimum and the application
window. Title and actions remain fixed while the form scrolls. Size persists across
form changes, including opening/cancelling deletion confirmation; writes disable
the handle. Dialog dimensions and the resize label are no longer vacancy-specific.

Verification: all 291 JVM tests, screenshot verification, ktlint, Detekt, coverage
verification, and macOS packaging passed. Mouse tests cover both axes, minimum and
window limits, reverse dragging, preserved input/size, deletion confirmation, and
disabled handles during writes. Project-dialog goldens were updated and reviewed,
including an expanded editor. The initial create-dialog dimensions remain unchanged.


## Structured resume content — 2026-10-12

Resume.content now holds a typed ResumeContent document. Profile fields are separate
from ordered sections: summary, grouped skills, experience, education, languages,
soft skills, contacts, and custom text. Section titles and visibility are editable
data; professional projects live within experience and use ResumeProject to avoid
confusion with workspace projects. Empty drafts and partial entries are valid.
Dates support year-only or year/month precision and explicit ongoing periods.

Room keeps the existing TEXT content column; ResumeEntity.contentJson stores a
formatVersion/content JSON envelope. Database mapping handles serialization and
rejects malformed data, unknown fields/types, and unsupported versions instead of
silently replacing content. The database schema version remains 1. Existing plain-text
resume content is not migrated; there was no resume editor in the application.
This supersedes the earlier plan to start with a single free-text editor.

Verification: all 298 JVM tests, unchanged screenshot references, ktlint, Detekt,
coverage reports/verification, and macOS packaging passed. Tests cover full/empty/partial
content, section order and visibility, Unicode and multiline text, year-only/ongoing
dates, malformed or unsupported JSON, entity mapping, independent adaptations,
keyword updates, and persistence after reopening the database.


## Resume JSON Schema — 2026-10-12

The version 1 storage contract is published in docs/schemas/resume-content-v1.schema.json
using JSON Schema Draft 2020-12. Its root covers formatVersion/content, and the
ResumeContent definition is reusable independently by future processors. Optional
fields match serializer defaults; date precision, current-period constraints, typed
section discriminators, and rejection of unknown fields are explicit.

NetworkNT validation runs only in JVM tests. Tests check the schema against its
meta-schema, validate full/empty/partial documents and negative cases, and recursively
compare fields, requiredness, nullable types, lists, and section variants against
Kotlin serializer descriptors. Production storage still uses Kotlin serialization.

Verification: all 302 JVM tests, screenshot verification, ktlint, Detekt, coverage
verification, and macOS packaging passed. The schema is loaded from repository
resources by the standard test suite; its validator is not packaged with the app.


## Resume list page — 2026-10-12

The entire Resumes dashboard card opens the current project's resume list. The
list has a Back header, scrolling rows with name and added date, and fixed footer
controls. Narrow panes and increased UI scale place dates below names. Loading
uses the shared delayed indicator; failures show Retry in the footer and log the
operation with projectId. Repository updates are observed until component disposal.

Page Back, Escape (including focus in project navigation), and hardware Back return
to the same dashboard while retaining project selection. Add is deliberately disabled
until the resume creation flow is implemented; rows do not open an editor yet.
The added-date formatter and date-column width token are shared with vacancies.

Verification: all 315 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed. Added references cover populated
and empty lists, load failures, both themes, narrow panes, and 150% scale. Dashboard
references were updated for the clickable Resumes card. Tests cover live updates,
observation cancellation, retry/logging, fixed controls, card clicks, and return
to the dashboard using page, Escape, and hardware Back.

## Structured resume creation — 2026-10-12

Add in the resume list opens a full-page creation component within the application
shell. The form edits a version name, comma-separated keywords, profile, and every
version 1 section type: summary, grouped skills, work experience (including professional
projects and categorized tools), education, languages, soft skills, contacts, and
custom text. Sections have editable titles and export visibility, can be reordered
or removed, and expand/collapse with animation. Repeated entries can be added or removed.

Only the version name is required; incomplete documents can be saved. Work and
education dates accept blank, YYYY, or YYYY-MM. Present omits the end date from
the stored document while retaining the typed end in the draft if toggled off.
Invalid dates remain editable, mark their fields/section, and prevent saving.

The component owns the draft; repository writes run through ResumesStore with
progress/failure labels, duplicate-write protection, and contextual error logging.
Failed writes retain the form for retry. Back, Cancel, Escape, and hardware Back
return to the resume list; saving blocks dismissal, edits, and project-tree navigation.
Saving stores typed content through the existing versioned JSON codec and refreshes
observed lists/dashboard counts. The repository activates Workspace on first creation.
Editing/deleting saved resumes and choosing a main resume remain planned work.

Verification: all 340 JVM tests, ktlint, Detekt, coverage reports/verification, and
macOS packaging passed. Creation tests cover all schema section types, partial dates,
ordering/visibility, validation, failed-write retry, duplicate saves, Back behavior,
and database persistence after reopening. Goldens cover the full shell, narrow panes,
150% scale, light/dark themes, missing name, save failure, and individual section editors.
Resume-list goldens were updated for the enabled Add button.

## Comma-separated resume skills — 2026-10-13

Skills and Soft skills now use single-line Items (comma-separated) fields, like
vacancy keywords. Drafts retain spaces and trailing separators while typing; saving
trims each item and drops empty entries. The schema still stores arrays of strings.
Updated the two section references and added a UI test covering both skill types.

## Saved resume editing — 2026-10-13

Clicking a resume name or added date opens the existing full-page form with an
Edit resume header and Update action. ResumeDraft restores profile, keywords,
all section types, order, visibility, professional projects/tools, and partial
or ongoing dates. Updating changes the existing record while preserving its ID,
project, creation timestamp, and sourceResumeId; the repository refreshes updatedAt.

Cancel, Escape, and hardware Back discard edits and return to the list. Failed
updates preserve the draft for retry and log projectId/resumeId with the stack trace.
Saving retains the shared duplicate-write and navigation guards. Editor footer
actions were extracted into ResumeEditorActions for reuse by both modes.

Verification: all 350 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed. New tests cover row clicks in
wide/narrow layouts, full structured round trips, section edits, cancellation,
failed-write retry, duplicate updates, lifecycle cancellation, and persistence
after reopening without creating a duplicate or changing provenance. Added
goldens cover editing in the application shell and a failed update in light theme.

## Main resume selection in the editor — 2026-10-13

Creation and editing now show a Main resume checkbox with an explanation of the
single selection per project. The form starts unchecked for new resumes and
restores the current Project.mainResumeId when editing. Save/Update selects this
resume, replaces the previous choice, or clears the choice when unchecking its
current owner. Cancel discards the selection draft; ordinary resume saves retain
another resume's main status. Selection remains a project reference, outside the
resume document JSON.

ResumesRepository observes the project's main ID and supports atomic document/choice
writes. ResumeDao wraps creation/update and main selection in one transaction;
failed selection rolls back content, keywords, inserted records, and project changes.
Clearing is conditional on the saved resume still owning the selection. The resume
list Store observes both records and the main ID so subsequent editors open with
the current choice. Profile fields and the role control have separate UI files.

Verification: all 355 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed. Tests cover creating, replacing,
clearing, preserving another selection, reopening the database, Cancel, checkbox
disablement while saving, and rollback after an injected SQL failure. Form goldens
were updated for checked/unchecked choices, light/dark themes, narrow panes, errors,
and increased scale.

## Resume management and permanent deletion — 2026-10-13

The saved-resume page is now titled Resume and has a critical-colored Delete action
on the left of its fixed footer. Delete opens the shared resizable dialog, identifies
the saved resume, warns that deletion is permanent, and requires DELETE (case-insensitive,
with surrounding whitespace ignored). Physical Enter confirms valid input. Cancel,
Escape, and hardware Back dismiss confirmation before leaving the management page.
Creation has no Delete action. Deletion does not depend on unsaved name/content validity.

The Store calls the existing permanent repository deletion and returns to the list
after success. Pending deletion blocks duplicate requests, editing, dismissal, and
project navigation. Failures retain confirmation for retry and log projectId/resumeId
with a stack trace. Owned document content and keywords are removed, main-resume
selection and vacancy/source references are cleared, and the last workspace record
returns the project to Unconfigured. Independent vacancies, adaptations, letters,
and their files remain. Resumes currently have no file-attachment model or storage.

Verification: all 362 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed. Added tests cover validation,
physical Enter/Escape, creation guards, Back ordering, failed deletion/retry,
duplicate-write protection, busy confirmation, and clearing the last main resume.
Existing persistence tests verify permanent deletion and relationship cleanup.
New dialog goldens cover empty confirmation and a failed deletion in light theme;
management-page references were updated for Resume and Delete.

## Shared confirmation dialog — 2026-10-13

ConfirmationDialog is a reusable UI element configured with localized title/message,
subject, button labels, optional input, and Standard/Critical appearance. Its state
contains component-owned input, confirmation availability, progress, and an optional
error. It reports typed Back, Cancel, or Confirm actions; the owning component decides
navigation, validates input again, and performs the operation. The shared UI has no
resume or DELETE-specific validation.

ResumeEditorComponent supplies its deletion copy and maps dialog actions to its
existing guarded handlers. ResumeDeleteConfirmation only observes feature state and
connects it to the common element. Hardware Back uses the same action route. Existing
resume confirmation layout and golden references remain unchanged.

Verification: all 367 JVM tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed. New independent tests cover the
three action types, caller-defined validation (READY), optional input, and busy guards.
Added common-element goldens cover standard confirmation without input and critical
confirmation with input/error. Existing resume deletion and navigation tests still pass.

## Stable resume ordering — 2026-10-13

Resume observation and keyword search now order by ascending ID. Names and update
timestamps do not change list position. Added a repository regression test with
conflicting names/timestamps, project scoping, and an edit of the middle record.
The resume repository tests, ktlint, and Detekt passed.

## Resume export dialog placeholder — 2026-10-13

Saved resume management now has an Export action; creation does not. The dialog offers a
format dropdown with as JSON selected by default. Cancel, Download, and Escape
only dismiss the dialog; no document is generated or saved. Added behavior tests
and dark/light dialog goldens, and updated page references for the new action.

Verification: all tests, screenshot verification, ktlint, Detekt, and coverage
verification passed. Updated a pre-existing persistence-test expectation to match
the ascending resume ID order.

## Save before resume export — 2026-10-13

Export now runs the same validation and persistence as Update, then opens the
export dialog only after success without leaving the management page. Invalid
drafts and storage failures keep the dialog closed; failures remain retryable.
Pending saves block duplicate requests and navigation. Cancel, Download, and Back
only close the export dialog; actual file generation is still unimplemented.
The saved main-resume role is refreshed so later edits can clear a selection made
during export. Component tests cover validation, delayed writes, failure/retry,
duplicate requests, Back ordering, and main selection after an export.

Verification: all tests, goldens, ktlint, Detekt, and coverage verification passed.

## Resume JSON file export — 2026-10-14

Download generates the saved resume content using the existing versioned JSON codec
and schema. Desktop export creates a UTF-8 temporary file before opening the native
save picker, suggests resume-{id}-{yyyyMMddHHss}.json using local generation time,
and copies the document to the chosen path. The timestamp follows the requested
format, which omits minutes. Temporary files are removed after saving, cancelling,
file/picker errors, and coroutine cancellation, including lifecycle cancellation.

The export dialog shows a spinner and disables controls/dismissal for the complete
operation. Success and picker cancellation close only the dialog. Failures show a
retryable error and log the operation, project ID, resume ID, and stack trace.
Platform export is supplied through ApplicationDependencies; editor lifecycle owns
the operation. File tests cover content round-trip, filename, ready-before-picker
ordering, overwriting, and cleanup. Component/UI tests cover progress, duplicate
requests, errors/retry, and blocked dismissal; goldens cover progress and failure.

Verification: all tests and goldens, ktlint, Detekt, coverage reports/verification,
and macOS packaging passed. The spinner golden freezes the animation clock after
the dialog entrance transition and was verified again independently.

## Resume management Save and Close — 2026-10-14

Saved resume management uses Save and Close. Successful saves keep the same editor
open and show green Success! in the feedback area for three seconds. Editing clears
the message immediately; each successful save restarts its lifetime, even when
fast saves occur within one UI frame. Success colors are theme-specific. Close
returns to the list without writing subsequent unsaved changes. Creation retains
Save/Cancel and returns to the list after creating a record. Export still saves
before opening its dialog; repeated saves preserve main-resume selection behavior.

Updated component tests for retained editing and main selection, added UI coverage
for transient/repeated feedback and Close, and added a success golden.

Verification: all tests, screenshot verification, ktlint, Detekt, coverage
reports/verification, and macOS packaging passed.

## Reserved resume feedback row — 2026-10-14

Resume management and creation reserve a single line above the footer buttons for
notifications. Save failures, date validation messages, and transient success
feedback use the same row. Its height follows the typography tokens and stays
allocated when empty; long messages scroll horizontally instead of wrapping.
Messages no longer change the form's position or viewport height. A UI regression
checks the form, notification row, and Save button bounds across empty, success,
and error states. Updated the editor goldens for the reserved footer space.

Verification: all tests, goldens, ktlint, Detekt, and coverage verification passed.

## Resume export filename timestamp — 2026-10-14

Export filenames now use resume-{id}-{yyyyddMM}-{HHmm}-{ss}.json: year, day of
month, month, then 24-hour time with minutes and separate seconds. Lowercase dd
represents the day of the month; uppercase DD would represent the day of the year.

## Keyboard Back after hovering action buttons — 2026-10-14

FillButton hover clears control focus. The page Back handler previously had no
focused event target afterwards, so Escape stopped working until another click.
The shared handler now restores focus to its page container only when the page
has no focused descendant and its window is active. Focused fields/buttons keep
keyboard input, and disabled/busy handlers do not reclaim focus. A regression
reproduces hover over resume actions without clicks, then verifies Escape closes
management without saves, deletes, or opening export.

Verification: the regression failed before the fix and passed afterwards. All
tests, unchanged goldens, ktlint, Detekt, and coverage verification passed.

Additional hover-without-click checks verify Escape closes vacancy management and
the Create project dialog without saving or deleting. Those actions currently use
TextButton, whose hover does not explicitly clear focus; both screens still benefit
from the shared Back-handler fallback. Their UI/focus/navigation tests and linters passed.
