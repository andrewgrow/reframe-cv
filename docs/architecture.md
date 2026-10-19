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
  lazily supplies the shared database, repositories, feature use cases, file services, logger,
  and Store factory.
  `AppComponentContext` carries these dependencies through the component tree.
- Components and MVIKotlin executors use feature use cases for data operations. Components
  retain UI state, lifecycle-bound jobs, navigation, feedback, and draft-file ownership.
  Use cases coordinate repositories and platform file services without presentation types.
  Desktop implementations own filesystem access, image processing, and file pickers.
- `domain` owns application models, use cases, repository/provider contracts, and resume document
  serialization. It depends on neither Room, Ktor, Compose, nor the application module.
- `database` depends on `domain` and owns Room entities, DAOs, converters, local repositories,
  and the exported schemas in `database/schemas`. Database construction accepts a path;
  desktop application dependencies select the application data directory.
- `network` owns Ktor transport, platform engines, common timeouts, and the JVM rotating
  diagnostic file writer reused by HTTP and application error logging; it has no knowledge
  of concrete AI providers or vacancy recognition DTOs.
- `providers` depends on `domain` and `network`. External repositories call an injected
  `AiProviderClient`, whose contract is in `domain`. Provider implementations own service-specific
  requests/responses and use the injected HTTP transport. There is no separate API module.
- `shared` common code depends on domain use cases; desktop composition wires database,
  provider, and transport implementations. Feature groups expose only their relevant actions:
  projects, resumes, vacancies, document/photo operations, dashboard, and recognition.
- Storage-only tests live in `database`, response/schema tests in `providers`, and editor/storage
  integration and UI tests in `shared`. Desktop window persistence remains infrastructure
  and uses `AppSettingsRepository` directly.
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
- A vacancy can link multiple resumes, one cover letter, and one immutable VacancyAttachment.
  `vacancies_resumes` stores ordered, unique vacancy–resume pairs and an integer ATS score
  from 0 to 100 per pair (default 0). Resume and vacancy deletion cascade their links;
  unlinking keeps the resume itself. Document and link changes share a save transaction.
- DAO operations validate existence and same-project document relationships.
  Main-resume assignment and document writes are atomic.
- Keywords use per-document indexed tables, with trimmed, nonempty, case-insensitively
  deduplicated values. Document and keyword writes are atomic; lookup is case-insensitive.
- Deletion is permanent; there are no soft-delete timestamps. Project deletion removes
  its subtree and workspace contents. Resume deletion clears main-resume, provenance,
  and vacancy references; letter deletion clears vacancy references. Independent content
  in surviving documents is retained.

## Persisted screen state

`ScreenState` stores arbitrary JSON view-state fields in one `screen_states` row per
screen and document. Standalone pages such as Settings have no document ID. Document
screens also retain their project ID for ownership checks and cascading cleanup.
The vacancy screen currently understands `job_details: "collapsed" | "expanded"`.
Screens interpret their own fields: unknown keys are preserved, invalid field values
fall back independently, and malformed or non-object JSON loads as an empty state.
Updating a field merges it into the existing object atomically, including unknown
nested objects, arrays, numbers, and booleans. There is no separate row per section.

`ScreenStateUseCases` serializes reads and patches for the application lifetime.
Reopening a page waits behind pending writes; navigation does not cancel them.
Application dependencies flush the queue before closing the database. Storage failures
are logged and do not affect document saving. New unsaved documents keep local state;
a successful first Save binds that state to the new document ID.

Project deletion cascades its screen states; vacancy, resume, and letter deletion
clear their own entries in the same transaction. Late writes for missing documents
are ignored. Screen state is excluded from document import, export, and recognition.

Vacancy Description, Responsibilities, Requirements, and Nice to have are grouped in
Job details. New documents default to expanded. The shared CollapsibleSection animates
visibility and retains field resize state across collapse and expansion. Restored state
loads before showing the contents to avoid briefly exposing a collapsed section.
The restored state appears without animation; only user toggles animate.

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

[Vacancy recognition schema](schemas/vacancy-recognition-v1.schema.json) defines
the Reframe CV contract for extracted job posting content,
not a saved vacancy: nullable jobTitle, jobDescription, company/URL,
responsibilities, requirements, and nice_to_have. All fields are required; missing
scalars are null and missing lists are empty arrays. Status codes and diagnostic
fields are not part of the model response. The schema ID identifies its version.

`VacancyRecognitionJson` rejects unknown fields and invalid types. Canonical
nice_to_have is an array; legacy text/null remains readable for compatibility.
Mapping trims fields and removes null, blank, and duplicate list items. Results
without a title, description, responsibilities, or requirements are NotRecognized.

`RecognizedVacancy.applyTo` replaces posting fields in the draft, including clearing
missing values. Internal name and manually entered keywords are preserved, including an empty name or keyword list.
Keywords are not part of the recognition contract. IDs, timestamps, linked
documents, and the pending attachment are preserved. Save remains explicit.

Vacancy now stores responsibilities, requirements, and niceToHave as ordered string
lists, defaulting to empty. Room persists them as non-null JSON array columns through
`VacancyListConverter`; prose is not split on commas. The existing indexed keyword
tables remain the search mechanism. The vacancy form exposes each list as a vertically
resizable multiline field, with one item per line. Drafts retain empty lines and
whitespace while typing; saving trims items and skips empty lines. Import and
recognition populate the same editable fields.
Vacancy lists and keyword search results use ascending IDs; edits do not reorder them.

## PDF vacancy recognition

`domain` defines the document input, typed outcomes/errors, and the recognition repository
contract. Successful recognition returns `RecognizedVacancy`, which has no provider status
codes or JSON annotations. `providers` owns `VacancyRecognitionResult`/`VacancyRecognitionJson`
in `reframecv.providers.recognition`; response mapping normalizes posting fields and converts
provider failures to domain errors before components receive them.

`RecognizeVacancyUseCase` prepares the PDF through `VacancyDocumentReader`, checks cancellation,
then calls `VacancyRecognitionRepository`. The external repository constructs a generic
`AiRequest` with document bytes, instructions, and the packaged recognition schema. Its injected
`AiProviderClient` returns JSON or a typed provider error; the repository validates/decodes JSON
and maps it to recognition outcomes. It neither saves drafts nor selects a concrete provider.

Desktop application dependencies compose the reader, repository, and lazy `OpenRouterClient`
with Ktor transport from `network`. Closing application dependencies releases the initialized
provider client, HTTP client, and engine; repositories do not own the shared transport.

OpenRouter generation uses POST `/api/v1/chat/completions` with the selected
connection's ordinary API key and saved model. PDF bytes are Base64 file input;
response_format supplies a strict JSON schema and routing requires parameter support.
Management keys are never used for generation. HTTP/provider failures, malformed JSON,
truncation, refusal, and empty posting content remain errors in the AI dialog.
The network client has 15-second connection and 120-second request/socket timeouts.
HTTP diagnostics use the existing redacted, rotating log; PDF Base64 data is redacted.
Protocol references: [PDF inputs](https://openrouter.ai/docs/guides/overview/multimodal/pdfs)
and [structured outputs](https://openrouter.ai/docs/guides/features/structured-outputs).

Parse PDF accepts selected files before Save or available saved snapshots. The desktop reader
checks the extension and `%PDF-` header, reads on IO, and verifies stored file size/SHA-256.
Missing, changed, unsupported, and unreadable files have explicit error states. Header checking
is not full PDF validation or local text extraction. Original files are never modified.

Recognition only updates the draft; saving and copying attachments remain explicit. It replaces
posting fields, preserves the user's Internal name and keywords, and keeps the
pending attachment. The attachment's nullable `parsedAt` stores successful recognition time.
The green indicator beside Parse PDF appears immediately after success and persists only
with the vacancy's Save. Reopening restores it; editing posting fields leaves it intact.
Replacement/detachment clears it, while failures/cancellation do not create a marker.
Existing-source parsing updates metadata in the same transaction as the vacancy save;
a stale attachment ID rejects the write. This metadata is excluded from portable JSON.
Failures leave fields unchanged and are retryable. Unexpected failures
include operation context and stack traces in the application log.

Processing disables draft edits, attachment changes, import, save, export, and deletion.
Cancel parsing or Back/Escape cancels the operation and keeps the editor open. Destroying the
editor cancels it too; results arriving after cancellation/disposal are discarded. Successful
results do not cause automatic saves or navigation. Only filenames and bytes enter the remote
contract; local file paths and application IDs are not part of it.

## Vacancy resume links

Below source recognition, the vacancy editor lists linked resumes and one empty picker.
The picker searches current-project resumes by partial, case-insensitive matches in
names and keywords, excluding already linked resumes. Choices are observed live and
load failures expose Retry. Each linked row has Unlink, its ATS percentage, and Assess.
Unlink uses the shared confirmation dialog without typed confirmation. Changes remain
in the vacancy draft until Save; closing the page discards them. Assess is a UI placeholder
and does not send an AI request or change the score. Recognition and file import preserve
these local links; portable JSON does not include links or ATS scores.

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
management with the persisted ID and timestamps. Later saves also keep the editor open;
returning to the list requires an explicit navigation action.
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

The Projects header stays visible above the scrollable tree and includes a collapse control.
Collapsing animates the sidebar to an icon rail containing only Expand and Settings;
its state is persisted as Application / navigation_panel in ScreenState, survives restarts,
and is restored before rendering the shell without an initial width animation. Expanding
restores the tree selection and expanded branches.


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
- Empty project pages place Delete on the left and Configure/Add project on the right,
  wrapping actions on narrow screens. Delete reuses the project editor's warning and
  case-insensitive DELETE confirmation; cancellation keeps the page, successful deletion
  returns to the parent list, and failures keep the confirmation available for retry.

## UI, device settings, and diagnostics

`ReframeTheme` supplies explicit Light/Dark modes, custom semantic colors, and scaled UI
spacing, dimensions, and typography. `UiScale` ranges from 75% to 150% in 5% steps. Outlines
and animation duration remain fixed. User-facing scale controls are not connected yet.
Shared UI includes FillButton, SelectableText, resizable text fields/dialogs, confirmation dialogs,
and fast form scrolling. Components keep state; UI files are split by responsibility.

Window size/maximized state is persisted through the application settings repository;
window position/display selection is not restored. Device preferences share the local database.

Errors include operation context and stacks in `stacktrace.txt`, preceded by the latest
200 Store/tree events. Event summaries omit document contents and project names.
Logging must preserve coroutine cancellation. The in-memory event history resets each launch.

Both diagnostic logs use `RotatingLogFile`: a 10 MiB active file plus five archives per log,
numbered `.1` (newest) through `.5` (oldest). Rotation removes only the oldest archive and
does not depend on application restarts. A shared process lock serializes appends and rotation
across instances. Ordinary entries remain together; oversized entries span files at UTF-8
character boundaries. Existing oversized files retain their latest 10 MiB before appending.
The retained files for both logs total at most 120 MiB. File failures remain diagnostic-only.

## Current limits

Cover-letter storage/repositories exist, but its editor is not implemented. Resume adaptation
UX, PDF/print output, URL snapshot fetching, and source parsing remain deferred. There is no
photo data in JSON exports. Database migrations and platform targets beyond desktop are not
implemented. Test/screenshot environments and verification commands are documented in README.

## Global settings and API connections

Settings is a persistent footer entry in the left navigation, independent of project scrolling.
It opens in the main content area. Back/Escape closes the connection editor before leaving
Settings and returns to the previous destination. Back during model loading cancels the request
without discarding the draft; saves cannot be interrupted through navigation.

`provider_connections` starts empty. Users create OpenRouter connections with an optional
connection name, an API key, and an optional model ID. Each connection has an independent
Active/Disabled state; new connections default to Active. Status switches persist immediately
for saved connections and never affect other connections. Disabling cancels pending requests.
Ordinary Save preserves the stored status, preventing stale drafts from re-enabling a connection.
Keys are currently plain text in the local database, masked in form fields, and omitted from
model string representations and operation logs.

The list uses one row per connection: Provider, optional Connection name, and Status. Model
selection and Load/Refresh models share one row in the editor. Save keeps the same editor,
updates its persisted identity, and shows a green notification for three seconds in a reserved
footer line. Further saves update the same row. Closing still discards unsaved field edits.

`ProviderSettingsUseCases` separates storage from provider model discovery. The external
`RemoteProviderModelsRepository` routes discovery to an injected `ProviderSetupClient`. OpenRouter setup
validates credentials with GET `/api/v1/key`, then reads `/api/v1/models/user`. The list is searchable;
model metadata identifies advertised structured-output support without guaranteeing endpoint
compatibility. Saving requires only a nonblank API key; checking credentials and loading
or choosing a model are optional, independent operations. Save cancels pending model discovery,
and late results cannot replace the saved draft. Changing the key clears the loaded catalog and
account cache but keeps the configured model ID. Catalog refreshes never clear that ID implicitly.
Only explicit Save writes the draft. Failures retain fields and display safe error categories; raw HTTP errors/bodies are
not exposed in the UI. Tests use MockEngine and do not send credentials or documents to a real service.

OpenRouter setup receives an injected `HttpTrafficLogger` from `network`. Desktop wiring
appends timestamped requests and complete text responses to `openrouter-http.log` beside
the database and mirrors them to Klogging (`OpenRouterHttp`, INFO), before DTO parsing or
HTTP error handling. Unknown JSON fields remain visible;
authorization, cookies, secret JSON fields, and the current API key are redacted. Logging
failures must not change API results, and coroutine cancellation remains cancellable.

Saved active connections can be selected for PDF recognition; generation uses their saved model.

`ProviderAccountClient` exposes account details separately from model discovery. The external
`RemoteProviderAccountRepository` routes to the selected implementation; OpenRouter reuses
GET `/api/v1/key` and maps budgets, the effective budget, key expiry, and fetch time to
`ProviderAccountInfo`. Older responses fall back to the key's limit/remaining fields and
usage for its configured reset period. Missing amounts or dates remain unknown; limits
from different scopes are displayed separately, never added together.

`provider_connections.account_info` stores the last successful typed snapshot as nullable JSON,
without the raw response or provider account identifiers. Refresh writes only the matching
saved ID/type/key and does not change the connection's edit timestamp. Saving a stale draft
preserves the latest cache for unchanged credentials; changing credentials clears the cache.
New connection drafts retain account details in memory until explicit Save.

The connection editor displays cached values immediately. It refreshes stale information on
resume and every five minutes while visible, with a manual Refresh action. Requests use the
same sanitized HTTP log as model discovery. Pausing, changing the key, or closing the editor
cancels pending work; cancelled/late responses cannot update drafts or storage. Refreshing
does not block editing or navigation. Failures retain cached values and show separate feedback;
timestamps are displayed explicitly in UTC. Tests use controlled ticks rather than real waits.

Provider reference: [OpenRouter current-key endpoint](https://openrouter.ai/docs/api/api-reference/api-keys/get-current-key).

Provider settings use cases check the connection's enabled state before and after remote calls,
including a fresh lookup of saved connections. A stale caller cannot bypass a persisted disable.
The account controller skips disabled connections on timer ticks; manual network actions are
also disabled. Account-cache writes require `active = 1`, so a response racing with disabling
cannot update a disabled row. Cached values remain readable while disabled. Recognition is
subject to the same access rule, checked before preparation, before sending, and before applying results.

OpenRouter connections may also store an optional management API key. It is used exclusively
for GET `/api/v1/credits`, never for model discovery, ordinary-key budgets, or document requests.
Account credits (`total_credits - total_usage`) have a separate typed cache and fetch timestamp;
they are not spending limits. Management keys use the same local plain-text storage and masked
input policy as ordinary keys, are excluded from model string representations, and are redacted
from HTTP traffic. Blank management keys produce no credit requests. Credit failures preserve
cached values and show independent feedback without blocking ordinary operations or Save.
OpenRouter errors expose only the structured `error.message`, with the request key redacted,
for credit refresh, key information, model discovery, and AI requests. Missing, blank, or
malformed messages fall back to the existing localized feedback; raw responses and transport
exception messages are not displayed. Retrying or changing credentials clears stale details.

Account credits share the editor's visible-only refresh schedule but have independent TTLs,
request jobs, and failures. Disabling, pausing, closing, saving, or changing credentials cancels
pending requests. Changing the management key invalidates only the credit cache; changing the
ordinary key invalidates only its budget cache. Credit-cache writes require matching provider,
management key, connection ID, and enabled status; stale saves preserve newer cached values.

The provider editor separates Management (management key and account credits) from Model API
(ordinary key, model catalog, and key budgets). Both sections have independent manual refresh
actions; connection identity and enabled status apply to the entire connection.

## Reusable prompts

Settings owns a prompt list and a separate editor. A prompt stores enabled status, optional
title, optional component scope (Resume, Vacancies, Cover letters, Projects), instructions,
and creation/update timestamps. No scope means available to every component. The management
list includes disabled records; feature selection must use `PromptsUseCases.observeAvailable`,
which excludes disabled prompts and includes both global and matching component-scoped records.

The prompt collection starts empty. Users create all prompts themselves; observing the list
or saving a prompt never inserts additional records. Existing prompts are retained unchanged.
Records are ordered by ascending ID. Save requires nonblank
instructions, trims outer whitespace, retains internal formatting, and keeps the editor open with
a temporary success notification. Repeated saves update the same record; Close/Esc discard edits.
Status changes are draft fields until Save. Errors retain the draft and are logged with operation
context. JSON schemas remain application contracts, separate from editable prompt instructions.

The prompt manager does not execute AI requests. `AiRequestDialogComponent` supplies
active connections and component-scoped/general prompts to processing screens. The
vacancy page opens it through Parse PDF, displaying the source filename, provider,
saved model, and a replacement warning. The dialog executes a caller-supplied operation
and returns only a successful typed result; the caller applies it to its draft.

Prompt text in Settings and the AI dialog has a vertical resize handle shared with the vacancy Description field.
Dragging at the viewport edge grows the field and scrolls its surrounding content.
The selected height survives text edits; resizing is disabled during a request or prompt save.
Prompt text edits are request-local unless Save prompt is pressed. Errors remain in
the dialog without discarding selections or text. Cancel/Escape cancels the operation
and closes only the dialog; late results are discarded. A single available choice is
selected automatically; multiple choices require selection. No selection is persisted.
The database schema is unchanged by AI request handling.

The Settings overview exposes matching text navigation entries for API Providers and Prompts.
Provider connections and Add provider live on the API Providers page. Back/Esc returns from
the connection editor to that list, then to the Settings overview, then out of Settings.

## Document footer alignment

Resume and vacancy editors share DocumentFooter, reserving a notification line and
one standard action row using the current typography and spacing tokens. The root
navigation footer uses the same minimum while an editor is active and follows its
measured height if wrapped buttons or additional messages require more room. Returning
to a list restores the normal navigation footer height. Notification content does not
shift the divider while it fits the reserved space.
