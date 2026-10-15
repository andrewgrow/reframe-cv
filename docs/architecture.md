# Architecture and constraints

This document describes current decisions, not development history or an implementation
sequence. Update it in place when a contract changes and remove superseded statements.
Code and tests define the actual behavior. Deferred work belongs in the [backlog](backlog.md);
build and verification commands belong in the [README](../README.md).

## Runtime and dependency boundaries

- The current application is local, single-user, and targets Desktop (JVM).
- Compose Multiplatform renders the UI. Decompose components own navigation and
  lifecycle; MVIKotlin stores/executors coordinate observable state and repository work.
- `ApplicationDependencies` is the platform dependency contract. The desktop implementation
  lazily supplies the shared database, repositories, file services, logger, and Store factory.
  `AppComponentContext` carries these dependencies through the component tree.
- Components use repository/service contracts rather than constructing databases or native
  dialogs. Desktop implementations own filesystem access, image processing, and file pickers.
- Domain models are separate from database entities. `DatabaseModel` provides domain mapping;
  `DatabaseModelFactory` constructs database representations. Local IDs and timestamps are
  `Long`; timestamps are Unix milliseconds, obtained through `nowMillis()`.

## Projects and workspace invariants

Projects form a tree through nullable `parentId`, with one parent per project.
The current UI supports navigating and creating descendants, not moving projects.

| Mode | Meaning |
| --- | --- |
| `Unconfigured` | No children or workspace records; either direction remains available. |
| `Container` | Contains subprojects and cannot contain its own workspace records. |
| `Workspace` | Contains workspace records and cannot contain subprojects. |

Opening Configure/dashboard does not persist a mode change. Creating the first child
or workspace record changes the mode in the same database transaction. Removing the
last child, or last record across all three workspace sections, returns the project
to `Unconfigured`. Repository/DAO validation enforces these rules beyond the UI.

## Persistence and relationships

Room with bundled SQLite stores projects, workspace records, keywords, source metadata,
and application settings. The development schema remains at version 1; there are no
migrations yet. Database recreation is a development policy, not an automatic startup
operation. See the README for reset and backup instructions.

- Resume, Vacancy, and CoverLetter belong to a project. Their relationships are optional;
  a vacancy or letter does not require a resume.
- `Project.mainResumeId` selects at most one resume from that workspace.
  `Resume.sourceResumeId` records provenance; adaptations are independent documents.
- A vacancy can reference one resume, one cover letter, and one immutable ImportRecord.
  Multiple vacancies can share those records; reverse relationships are queried.
- DAO operations validate existence and same-project document relationships.
  Main-resume assignment and document writes are atomic.
- Keywords use per-document indexed tables, with trimmed, nonempty, case-insensitively
  deduplicated values. Document and keyword writes are atomic; lookup is case-insensitive.
- Deletion is permanent; there are no soft-delete timestamps. Project deletion removes
  its subtree and workspace contents. Resume deletion clears main-resume, provenance,
  and vacancy references; letter deletion clears vacancy references. Independent content
  in surviving documents is retained.

## Resume document contract

`Resume.content` is typed `ResumeContent`, stored as versioned JSON. It contains profile,
ordered typed sections, document language, and nested consent text/visibility. Section
visibility preserves content. Partial and ongoing dates have explicit model rules.
The [JSON Schema](schemas/resume-content-v1.schema.json) describes the storage/export
contract; its `$defs/ResumeContent` can be used independently.

Database decoding is strict. File import tolerantly keeps recognized fields and section
types, skips incompatible/unknown values, and rejects malformed or unsupported-version
documents. Import changes only the creation draft's content; name, keywords, main-resume
selection, and photo metadata remain intact. Save is explicit.

Export saves the draft first and generates formatted schema-compatible JSON from content.
Temporary import/export copies have explicit ownership and are cleaned on completion,
failure, or editor disposal as appropriate. Original user files are not modified.

## Managed file ownership

Database records store relative paths to managed copies beside the database. File writes
and database commits cannot form one SQLite transaction: coordinators serialize writes
and cleanup, remove uncommitted copies on failure, and delete replaced files after commit.
Files still referenced by another record must survive cleanup. Cleanup failures are logged.

### Vacancy snapshots

ImportRecord preserves original source data and metadata independently of editable vacancy
fields. Refetching/replacing a source creates another immutable record. File-backed records
store UUID copies in `attachments/`, original names, MIME types, byte sizes, and SHA-256;
`rawData` can be null. Missing/changed files retain metadata and expose an unavailable state.

Vacancy/project deletion removes affected sources after their last reference. Snapshot
orphan cleanup runs when vacancy observation starts and is serialized with writes.
URL fetching and parsing are not implemented.

### Resume photographs

`Resume.photoPath` and `photoVisible` are outside ResumeContent and JSON import/export.
Visibility defaults to true and only governs future print output; switching it off keeps
the file. Replacing/removing a photo and changing visibility are committed through Save.

The editor owns an orientation-corrected temporary source for manual framing. Shared crop
geometry drives the preview and 413 × 531 pixel, 300-dpi JPEG output (35 × 45 mm). Save renders
the crop before the database write; the old portrait survives a failed save. Sources remain
available for further adjustments within that editor and are discarded on disposal.
Only the finished portrait is retained after closing. Copies live in `documents/resume-photos/`.
Missing saved files remain replaceable/removable without discarding resume content.

## Navigation and editing lifecycle

- The root owns the project tree and right-pane destination. Tree expansion and selection
  are separate from content navigation; `ProjectPathEntry` represents hierarchy without
  visual breadcrumb rows.
- Back follows editor → list → dashboard → parent project list, rather than visit history.
  Pages expose Back/Escape; active dialogs handle dismissal first. Busy operations prevent
  leaving or submitting duplicate writes. Hardware Back uses the same component behavior.
- Resume and vacancy editors own drafts. Failures retain them; Close discards unsaved edits.
  A newly saved resume keeps the same component, draft, scroll position, and photo source,
  gains its persisted identity, and switches to management actions. Further saves update it.
  `createSaved` returns ID/timestamps without a fallible follow-up read after insertion.
- Loading is explicit; a spinner appears only after one second to avoid navigation flashes.
  Resume feedback reserves space so notifications do not move page controls.

## UI, device settings, and diagnostics

`ReframeTheme` supplies explicit Light/Dark modes, custom semantic colors, and scaled UI
spacing, dimensions, and typography. `UiScale` ranges from 75% to 150% in 5% steps. Outlines
and animation duration remain fixed. User-facing scale controls are not connected yet.
Shared UI includes FillButton, SelectableText, resizable dialogs, confirmation dialogs,
and fast form scrolling. Components keep state; UI files are split by responsibility.

Window size/maximized state is persisted through the application settings repository;
window position/display selection is not restored. Device preferences share the local database.

Errors include operation context and stacks in `stacktrace.txt`, preceded by the latest
200 Store/tree events. Event summaries omit document contents and project names.
Logging must preserve coroutine cancellation. The in-memory event history resets each launch.

## Current limits

Cover-letter storage/repositories exist, but its editor is not implemented. Resume adaptation
UX, PDF/print output, URL snapshot fetching, and source parsing remain deferred. There is no
photo data in JSON exports. Database migrations and platform targets beyond desktop are not
implemented. Test/screenshot environments and verification commands are documented in README.
