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
- A vacancy can reference one resume, one cover letter, and one immutable VacancyAttachment.
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

## Vacancy naming

`Vacancy.name` is the user's required label for identifying a saved opportunity; lists
show this label. `Vacancy.jobTitle` is the position title from the posting and may be
empty for manually entered vacancies. Drafts, persistence, and portable JSON retain both.
Recognition replaces posting content while preserving the user's label.

## Vacancy recognition contract

[Vacancy recognition schema](schemas/vacancy-recognition-v1.schema.json) adapts
`divo-resume/src/utils/schemas/parseJob.schema.json`. It describes a processing result,
not a saved vacancy: statusCode (200/400/500), status/statusDetails, nullable jobTitle
and jobDescription, nullable company/URL, keywords, responsibilities, requirements, and nice_to_have.
Its version is identified by the schema ID, independently of the database schema.

`VacancyRecognitionJson` reads this contract. Canonical nice_to_have is an array;
legacy text becomes one item and null becomes an empty list. Nullable list entries
remain representable in the response but are removed when applying it to a vacancy.
The original project used a string in its JSON schema and an array in its database
model; this compatibility rule resolves that inconsistency. Unknown fields and
invalid types/status codes are rejected.

`VacancyRecognitionResult.applyTo` accepts only successful results. An unknown jobTitle
becomes empty; a blank name must still be filled before saving.
It replaces jobTitle, company, URL, description, and every recognized list; unknown
values clear the corresponding fields. The user-defined name is preserved, or filled from
jobTitle when blank. IDs, timestamps, linked documents, and VacancyAttachment are preserved.
Processing status and provider response IDs are not vacancy business fields.
The mapping is explicit; it does not fetch, parse, call a model, or write to storage.

Vacancy now stores responsibilities, requirements, and niceToHave as ordered string
lists, defaulting to empty. Room persists them as non-null JSON array columns through
`VacancyListConverter`; prose is not split on commas. The existing indexed keyword
tables remain the search mechanism. Vacancy drafts preserve these lists even though
the current form does not expose their editing controls yet.
Vacancy lists and keyword search results use ascending IDs; edits do not reorder them.

## PDF vacancy recognition

`recognition-api` contains provider-independent input/result/error types and the repository
contract. `network` depends on that contract, owns Ktor/CIO client configuration, and exposes
the remote implementation. Shared UI code depends only on contracts; desktop application
dependencies compose the platform file reader and lazily created remote repository. Closing
application dependencies releases an initialized HTTP client and its engine.

The provider is deliberately unconfigured: the remote repository returns NotConfigured and
sends no HTTP requests. The network module has no endpoint, credentials, or provider payload
format yet. Client configuration provides 15-second connection and 120-second request/socket
timeouts; the actual provider integration and cancellation behavior need testing when added.

Parse PDF accepts selected files before Save or available saved snapshots. The desktop reader
checks the extension and `%PDF-` header, reads on IO, and verifies stored file size/SHA-256.
Missing, changed, unsupported, and unreadable files have explicit error states. Header checking
is not full PDF validation or local text extraction. Original files are never modified.

Recognition only updates the draft; saving and copying attachments remain explicit. It replaces
posting fields, preserves the user's name (or fills a blank name from jobTitle), and keeps the
pending attachment. Failures leave fields unchanged and are retryable. Unexpected failures
include operation context and stack traces in the application log.

Processing disables draft edits, attachment changes, import, save, export, and deletion.
Cancel parsing or Back/Escape cancels the operation and keeps the editor open. Destroying the
editor cancels it too; results arriving after cancellation/disposal are discarded. Successful
results do not cause automatic saves or navigation. Only filenames and bytes enter the remote
contract; local file paths and application IDs are not part of it.

## Vacancy JSON export

[Vacancy content schema](schemas/vacancy-content-v1.schema.json) defines the portable
`formatVersion: 1` document. Its content includes user-defined name, original jobTitle, company, description,
URL, keywords, responsibilities, requirements, and niceToHave. Local IDs, timestamps,
linked document IDs, file paths/bytes, and processing statuses are excluded.
This document is separate from the recognition response contract.

Export is available on saved vacancy pages and commits the current draft first,
including pending snapshot changes. Invalid names and failed saves prevent the dialog
from opening. After success, pending snapshot changes reset to Keep so later exports
do not recopy attachments. The same editor remains open after destination selection
or cancellation. First Save also keeps the same editor open, switching creation to
management with the persisted ID and timestamps. Later ordinary Save returns to the list.
Pending attachments bind to the new vacancy ID and reset to Keep after creation.

Resume and vacancy export share the JSON options dialog and desktop file lifecycle.
Download shows progress, generates formatted UTF-8 JSON before the native picker, and
suggests `vacancy-<id>-<yyyyddMM>-<HHmm>-<ss>.json`. Temporary files are deleted on
success, picker cancellation, write failures, and coroutine cancellation. Generation
and copying run on IO; errors keep the dialog retryable and include operation context.

## Vacancy JSON import

Creation offers Import for the same `formatVersion: 1` vacancy content document used by
export. Import ignores unknown fields, filters malformed known values/list entries, and
rejects broken JSON, unsupported versions, and missing/non-object content. Local IDs,
links, file paths, and attachment bytes are never imported. Recognition responses are a
separate contract and are not accepted as portable vacancy documents. Legacy content.title
is accepted as name when an explicit name is absent; exports use name and jobTitle.

Import replaces portable draft fields, preserving any pending source-file attachment.
It does not write to the database; Save remains explicit and name validation still applies.
Import blocks duplicate selection, editing, saving, and Back until selection/parsing ends.
Picker cancellation and failures preserve the current draft; errors are logged and retryable.

The desktop importer reads a temporary copy without modifying the original file. Failed or
cancelled imports remove their copies immediately. Successfully imported copies belong to
the editor and are deleted on Close or lifecycle destruction, including after first Save
switches the same page into management mode. Import is unavailable in management mode.

## Managed file ownership

Database records store relative paths to managed copies beside the database. File writes
and database commits cannot form one SQLite transaction: coordinators serialize writes
and cleanup, remove uncommitted copies on failure, and delete replaced files after commit.
Files still referenced by another record must survive cleanup. Cleanup failures are logged.

### Vacancy snapshots

VacancyAttachment represents an immutable attached file independently of editable vacancy
fields. Replacing a file creates another record. Attachments store UUID copies in
`attachments/`, original names, MIME types, byte sizes, SHA-256, creation timestamps, and an
optional original source URL. File metadata is required; missing/changed physical files
retain their metadata and expose an unavailable state. Provider identifiers, raw parser
responses, and arbitrary import metadata are not persisted. Incoming recognition results
map directly to Vacancy without an intermediate database record.

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
