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
- Resumes and letters initially store editable text.
- Opening the dashboard does not change the project's mode.
- Creating the first record in any section changes the project to `Workspace`.
- A project in `Container` mode cannot contain its own workspace records.
- A project in `Workspace` mode cannot contain subprojects.
- Record creation and the mode transition must occur in a single transaction.

## Navigation and screen layout

- The left pane shows a vertically scrollable project tree rooted at `Projects`.
- Tree entries are text rows with indentation for nesting, rather than menu buttons.
- Clicking an expansion arrow reveals or hides children; clicking a project name
  opens that project in the right pane. These are separate actions.
- Clicking `Projects` opens the root project list.
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

- [ ] Add `Resume`, `Vacancy`, and `CoverLetter` in separate model packages.
- [ ] Add the shared and model-specific fields listed above.
- [ ] Define defaults for optional fields and `keywords`.
- [ ] Follow the existing `DomainModel` contract and project conventions.

## Stage 2. Database and DAOs

- [ ] Add Room entities and mappings between database entities and domain models.
- [ ] Add keyword tables and the required foreign keys.
- [ ] Add indexes on `projectId`, relationship foreign keys, and keywords
  based on the actual queries.
- [ ] Add DAOs for creating, reading, updating, and soft-deleting records.
- [ ] Add observation of active records and their counts in the selected project.
- [ ] Add keyword searches for each entity.
- [ ] Implement transactional record creation that changes the project to `Workspace`.
- [ ] Reject record creation in a missing, deleted, or container project.
- [ ] Preserve the rule that `Workspace` projects cannot contain subprojects.
- [ ] Register the new entities and DAOs in `AppDatabase`.
- [ ] Regenerate the single version 1 schema and recreate the old local database
  if necessary. There is no real user data yet, so migrations are not required.

## Stage 3. Repositories and dependencies

- [ ] Add separate `ResumesRepository`, `VacanciesRepository`,
  and `CoverLettersRepository` contracts.
- [ ] Add local implementations that encapsulate DAOs and transactions.
- [ ] Provide the repositories through `ApplicationDependencies`.
- [ ] Add observation of the three section counts for the dashboard.
- [x] Provide observation of active projects for the navigation tree, including
  hierarchy and name changes.
- [ ] Validate that related records belong to the selected project.
- [ ] Reject relationships to missing or soft-deleted records.
- [ ] Use the existing time, error-handling, and lifecycle mechanisms.

## Stage 4. Project tree and dashboard navigation

- [x] Replace the root's left menu buttons with a project-tree component.
- [x] Model tree expansion and current selection independently from right-pane content.
- [x] Observe active projects and update the tree after creation, renaming, and deletion.
- [x] Wire tree-name clicks and the `Projects` root to existing Decompose navigation.
- [x] Expand ancestors and reveal the selected row when navigation originates
  from the right pane or Back.
- [x] Preserve expansion and scroll state when switching right-pane screens.
- [x] Define a consistent destination when the currently open project is deleted.
- [ ] Add a dashboard component with the project identifier and current project name.
- [ ] Pass the Configure callback from the component to `EmptyProjectActions`.
- [ ] Add the Configure transition through the existing Decompose navigation.
- [x] Preserve hierarchy-aware navigation and Back without visual breadcrumb rows.
- [ ] Open the dashboard when revisiting a project in `Workspace` mode.
- [ ] Preserve the current subproject list for `Container` mode.
- [ ] Preserve the Configure / Add project choice for `Unconfigured` mode.
- [ ] Observe counts and cancel observation when the component is destroyed.
- [ ] Distinguish loading, ready, and error states.
- [ ] Use the existing delayed loading indicator so zero counts are not shown
  as results before the initial read completes.

## Stage 5. Dashboard layout

- [x] Add the scrollable text-based project tree with expansion arrows,
  selection, hover, keyboard focus, and full-name tooltips.
- [x] Remove right-pane breadcrumb rows and repeated project titles.
- [x] Keep project names current in the left tree after renaming.
- [x] Keep the main content scrollable and the bottom controls anchored.
- [ ] Create a separate dashboard UI package alongside the other project screens.
- [ ] Put the screen container and reusable section block in separate files.
- [ ] Show three columns: Resumes, Vacancies, and Cover letters.
- [ ] Show actual active-record counts; empty sections display `0`.
- [ ] Use English string resources and the tokens and colors from `ReframeTheme`.
- [ ] Use `SelectableText` for ordinary text where it does not interfere with interaction.
- [ ] Support narrow windows and increased UI scale without clipping content.
- [ ] Avoid nonfunctional creation buttons: connect navigation and actions
  alongside their corresponding section screens.

## Stage 6. Verification

- [ ] Verify persistence and reading of all three models, including optional relationships.
- [ ] Verify independent creation of vacancies and letters without a resume.
- [ ] Verify reuse of one resume and letter across multiple vacancies.
- [ ] Verify keyword normalization, deduplication, and case-insensitive searches.
- [ ] Verify that lists, searches, and counts exclude soft-deleted records.
- [ ] Verify transactional mode changes: a failed write must not leave the project in `Workspace`.
- [ ] Verify that subprojects and workspace records cannot coexist in a project.
- [ ] Verify Configure, navigation back, and reopening a project in `Workspace` mode.
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
- [ ] Verify count updates and dashboard loading / error states.
- [ ] Add dashboard goldens in dark and light themes, with zero and nonzero counts.
- [ ] Add a separate golden for the reusable section block if its states need
  detailed coverage; do not duplicate all of them on the full screen.
- [ ] Verify narrow windows and increased UI scale.
- [ ] Run formatting, ktlint, Detekt, all tests, and coverage verification
  using the current README instructions; visually review new goldens.

## Open questions

- [ ] Define minimum name and content validation when creating records.
- [ ] Define relationship behavior when a resume, letter, or vacancy is soft-deleted.
- [ ] Define workspace-record behavior when their project is deleted.
- [ ] Decide whether a project stays in `Workspace` after its last record is deleted.
- [ ] Define searches with multiple keywords: match all or any.
- [ ] Agree on the dashboard layout for narrow windows.
- [ ] Agree on left-pane sizing and how deep nesting behaves in narrow windows.
- [ ] Decide how Back behaves when switching between unrelated tree branches
  and when returning from an unconfigured project's dashboard.
- [ ] Before implementing section screens, agree on their navigation, editors,
  and the workflow for creating a resume adaptation.

## Current state

The plan has been saved. Model and dashboard implementation has not started.
The project already has `ProjectMode` values `Unconfigured`, `Container`,
and `Workspace`; Configure currently has an empty click handler.
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

Next: implement the three domain models in Stage 1, then their storage and repositories.
The dashboard itself and Configure navigation remain pending.

Verification on 2026-10-05: all JVM tests, screenshot verification, ktlint,
Detekt, and coverage verification passed with:

```shell
./gradlew cleanJvmTest ktlintCheck detekt allTests :koverVerify --no-parallel --no-build-cache
```

Project-screen, root-screen, loading, empty-project, and UI-scale goldens were updated.
Isolated tree and project-list goldens were visually reviewed in dark and light themes,
including the expanded project help on a narrow screen.
