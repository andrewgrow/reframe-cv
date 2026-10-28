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
- `documents` owns Compose print templates and measured desktop PDF pagination; `shared`
  supplies the platform exporter and temporary-file lifecycle.
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
- A vacancy can link multiple resumes, multiple reusable cover letters, and one immutable VacancyAttachment.
  `vacancies_resumes` stores ordered, unique vacancy–resume pairs, a match score from
  0 to 100 (default 0), and optional assessment JSON. Resume and vacancy deletion cascade their links;
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
The vacancy screen understands `job_details: "collapsed" | "expanded"` and the positive
numeric prompt IDs `resume_assessment_prompt_id` and `resume_improvement_prompt_id`.
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
visibility preserves content. Profile includes optional free-text work arrangement and work
authorization fields, separate from location. Experience entries keep an introductory
description separate from achievement bullets and projects. Positions and projects each
own ordered labeled URL links (`ResumeLink`); education entries have optional notes for
honors and other details. The editor exposes all these fields and confirms link removal.
Summary has an editable section title. Summary text, position descriptions, and achievement
lists have vertical resize handles that scroll the form when dragged near its viewport edge.
Resizing preserves text and is disabled while the form is busy.
Partial and ongoing dates have explicit model rules.
The [JSON Schema](schemas/resume-content-v1.schema.json) describes the storage/export
contract; its `$defs/ResumeContent` can be used independently.

New optional content fields use empty-string/list defaults, so existing version-1 documents
remain readable without a database reset. The Room table structure is unchanged.

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
load failures expose Retry. Each linked row has Unlink, its match percentage, and Assess.
Unlink uses the shared confirmation dialog without typed confirmation. Ordinary link changes
remain in the vacancy draft until Save. Recognition and file import preserve these links;
portable JSON does not include links or assessments.

Assess opens the shared AI request dialog with active providers and vacancy/general prompts.
The provider receives the same versioned JSON content used by resume and vacancy exports as
two named text documents, never PDFs, attachments, photos, local paths, or persistence IDs.
The response contract is [resume-assessment-v1](schemas/resume-assessment-v1.schema.json):
only an integer `score` (0–100) and a nonblank `summary`. This is documented content fit,
not a prediction of an employer's ATS or hiring decision. Invalid output stays in the dialog
as an actionable failure; closing it cancels work and late responses are discarded.

For existing vacancies, the assessment use case atomically upserts only the assessed link
and result immediately, preserving other stored links and vacancy fields. It can attach a
resume linked only in the current draft. An assessment of a new vacancy stays in its draft
until Save. Save always writes the complete current draft again. Persistence failures keep
the request dialog open and do not apply a false success to the draft.

The saved JSON includes result, timestamp, provider/model, resume update timestamp, and the
exact portable vacancy JSON used for assessment. Clicking an assessed score reopens its
summary; a changed resume timestamp or vacancy JSON shows a stale-result warning. Reassessment
replaces only that link's result. Cascade deletion removes the assessment with its link.
No default prompt is seeded; users can copy the assessment example from prompt_examples.md.

## Resume list selection and vacancy links

The resume list offers per-row checkboxes and tri-state Select all independently of opening an
editor. Selection is local to the list component, survives visiting a linked vacancy, and prunes
identities removed by live updates. Delete selected requires a shared confirmation dialog with
the selected names, without typed input. Busy deletion blocks dismissal and competing actions;
failures keep selection and confirmation for retry.

Bulk deletion validates every ID against the project and deletes the set in one database
transaction. It reuses ordinary deletion cleanup for main-resume selection, source references,
vacancy links and ScreenState. Vacancies, derived resume content and cover-letter text remain.
Photo cleanup runs under the shared portrait mutex after commit, removing each unreferenced
file once and retaining any file still referenced by an unselected resume. Failed transactions
leave records and photos intact.

Linked appears before Added in the table; compact layouts show the links below the resume name.
A small live join projection supplies every linked vacancy's current internal name and ID in
vacancy-ID order. Each link opens that vacancy inside the resume navigation stack; Close/Back
returns to the same resume list and selection. These changes do not alter the database schema.

Resume list rows preview at most two vacancy links. Additional links are available through
a count link that opens a scrollable dialog with all linked vacancies. Opening a vacancy
from the dialog dismisses it first and retains the list selection for Back navigation.

## AI resume improvement

Each linked resume has an Improve icon beside its match score. It opens the shared AI
request dialog with active connections and vacancy/general prompts; the saved provider
model is used. Prompt changes remain request-local unless Save prompt is pressed.
Only portable resume and vacancy JSON are sent, without photos, attachments, resume
keywords, local IDs, or paths. Portable vacancy keywords remain part of its JSON contract. The schema derives from the existing resume document contract;
provider-compatible strict output makes object properties required, uses anyOf for section
variants, and leaves date invariants to domain validation. Generated content is decoded
strictly, without tolerant import repair or legacy consent normalization.

`ImproveResumeUseCase` creates a new resume in the same project with the selected source's
ID as `sourceResumeId`, a name formed as `<source name> + <vacancy internal name> (AI yyyy-MM-dd HH:mm)` in the device's local time,
and the generated content. Repeated trailing AI timestamps and matching vacancy-name suffixes
are replaced, so repeated improvement for the same vacancy keeps one vacancy name and one AI timestamp.
A blank draft vacancy name is omitted; other custom name parts are preserved. Keywords are empty;
main-resume selection is unchanged. The source photo path and Include in resume flag are inherited
locally without sending the photo to the provider. Versions share the immutable photo file;
replacing or deleting a photo removes its file only after the last resume reference is gone.
The source resume and its assessment are preserved. The new link starts at zero with no
assessment. Each version can be assessed and improved again independently.

For a saved vacancy, ResumeDao creates the adaptation and appends its link in one transaction,
preserving existing links, scores, and all other stored vacancy fields. Missing/foreign
vacancies or missing source resumes reject the operation without an orphan record. The
vacancy editor adds the result to its draft and immediately displays its saved name.
For a new vacancy, the adaptation is saved to the project immediately while its link remains
in the vacancy draft until Save. Closing that draft keeps the created resume in the project.
Later Save writes all current vacancy draft links again.

Provider, invalid-output, and persistence failures keep the AI dialog open for retry.
Closing the dialog or destroying its owner cancels work; cancelled remote results cannot
create an adaptation. HTTP exchanges reuse sanitized provider logging, and unexpected
failures include the AI operation context and stack in application diagnostics.

## Cover letters in vacancies

The vacancy editor groups resumes and letters into animated collapsible sections. ScreenState
stores `linked_resumes` and `linked_letters` per vacancy, restoring the initial visibility without
animation. New drafts bind their current visibility on first Save. Headers include link counts.

`vacancies_letters` stores ordered unique many-to-many links. Only letters in the same project
can be attached. Manual attaching/unlinking is part of the vacancy draft and is persisted by Save;
unlinking preserves the letter. Deleting a letter cascades all its vacancy links, while deleting
a vacancy keeps reusable letters. `CoverLetter.sourceResumeId` records the generating resume;
resume deletion clears this optional reference without removing the letter text.

Generate cover letter selects one linked resume (automatically when exactly one is available)
and opens the shared AI Request Dialog. Active Cover Letters/general prompts and active provider
connections are available. `cover_letter_prompt_id` remembers the last launched prompt per vacancy.
Only portable resume/vacancy JSON is sent, without resume photos, keywords, local IDs or paths.
The response contract is [cover-letter-generation-v1.schema.json](schemas/cover-letter-generation-v1.schema.json):
an object containing only nonblank plain-text `content`. The example prompt is user-managed.

Generation creates a new editable letter in the same project. For a saved vacancy, creation and
appending its link are atomic and preserve other stored vacancy fields and existing links.
For a new vacancy, the letter is saved immediately and its draft link waits for vacancy Save;
closing the vacancy keeps the generated letter. Provider, decoding and persistence errors remain
in the AI dialog for retry. Cancellation prevents late results from creating letters.

The open icon displays letter management within the vacancy navigation stack. Name, text and
comma-separated keywords can be edited; Save keeps the page open and shows transient feedback.
Text supports vertical resizing and shared fast-scroll controls. Close/Escape returns to the
same vacancy draft. Permanent deletion uses the shared confirmation dialog without typed input,
clears the current draft link, and returns to the vacancy. Generation never sends an application.

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
suggests `vacancy-<id>-<yyyyMMdd>-<HHmm>-<ss>.json`. Temporary files are deleted on
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
- Resume deletion uses a confirmation dialog with a second destructive button, without
  typed confirmation. Cancel/Escape keeps the editor; failures remain retryable and busy
  deletion prevents repeated confirmation or dismissal.
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

All three diagnostic logs use `RotatingLogFile`: a 10 MiB active file plus five archives per log,
numbered `.1` (newest) through `.5` (oldest). Rotation removes only the oldest archive and
does not depend on application restarts. A shared process lock serializes appends and rotation
across instances. Ordinary entries remain together; oversized entries span files at UTF-8
character boundaries. Existing oversized files retain their latest 10 MiB before appending.
The retained files for all three logs total at most 180 MiB. File failures remain diagnostic-only.

## Current limits

Resume version comparison, URL snapshot fetching, and additional source formats remain deferred.
There is no photo data in JSON exports. Database migrations and platform targets beyond desktop are not
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

Completed AI requests (including failures, excluding cancellation) trigger a best-effort background
credit refresh for the used connection. This application-scoped job survives the request dialog,
does not delay its result, and skips disabled connections or missing management keys. A successful
refresh updates the existing cache and shows Remaining balance above Settings for five seconds,
rounded to cents as `$99,99`; the collapsed panel shows only the amount. New successful refreshes
restart that timer. Failures are logged without changing the AI result, and shutdown cancels jobs.

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
selected automatically; multiple choices require selection unless a valid saved prompt
preference is available. Provider choices and PDF recognition prompt choices are not persisted.

Vacancy assessment and improvement remember their last launched prompt independently in
application-scoped ScreenState, shared across vacancies, projects and new drafts. Existing
per-vacancy choices remain a read-only fallback until an application-wide choice is present.
A validated request launch records the selected prompt ID, including attempts that later fail
or are cancelled; selecting a prompt and closing without running does not replace the
preference. Reopening uses the current saved prompt text, not previous request-local edits.
Deleted, disabled, wrong-scope or malformed saved choices fall back to ordinary selection
rules. Reads check persisted state before in-memory choices so retained editors observe
newer selections from other pages. Preference read/write failures are diagnostic-only and
retain the last used in-memory selection. Global preferences are saved without waiting for
vacancy Save; other vacancy view state and cover-letter prompt preferences retain their
existing document scope. Preferences share the screen-state write queue and survive
application restarts.
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

## Cover-letter list

The dashboard opens the project’s cover letters in ascending ID order. Selection is local to
the list and survives opening a letter or a linked vacancy. The Linked vacancies column
observes current vacancy links and names; removed links disappear immediately. Two links
are previewed per letter, with the remaining links available in a scrollable dialog. Bulk deletion validates the whole selection
and deletes it in one database transaction, clearing vacancy links and screen states while
keeping resumes and vacancies. The list can create letters manually or import JSON; AI generation remains available
from a vacancy.

## Cover-letter JSON exchange

[Cover-letter content schema](schemas/cover-letter-content-v1.schema.json) defines a versioned
`formatVersion`/`content` document with `name`, `text`, and `keywords`. Local IDs, timestamps,
source-resume IDs, and vacancy links are excluded. The AI generation schema remains a separate
response contract. Import ignores unsupported fields and malformed field values, requires a
supported envelope, and fills a new draft. Save persists it once and keeps the editor open.
Export saves current edits before opening the shared JSON download dialog. Native file pickers
use temporary copies; export removes them after completion or cancellation, and import retains
them until the editor closes. User source files remain unchanged.

## Cover-letter PDF export

Export saves the current letter and opens a format selector: PDF first and selected by default,
then JSON. Download invokes the matching file use case with the saved ID. A blank body blocks
PDF generation with feedback in the dialog; JSON remains available for incomplete letter text.
Generation and file copying run on a background dispatcher; the native destination picker runs
on Swing. The page stays open and blocks duplicate actions during the operation. The export
dialog shows progress and retryable errors; the editor shows PDF-save success. Cancelling the
destination picker keeps the saved letter without claiming a PDF was saved.

`DesktopCoverLetterPdfExporter` renders body text only: internal name, keywords, and local IDs
are excluded. `DesktopDocumentExport` owns temporary JSON/PDF files and removes them in a
non-cancellable cleanup block. Suggested names use `cover-letter-<id>-<yyyyMMdd-HHmm-ss>.pdf`.
PDFs are exported to user-selected destinations, without persistent attachment records.
The fixed A4 template uses measured page fragments to avoid compose2pdf's automatic-pagination
hidden-text duplication; see [PDF rendering](pdf-rendering.md).

## Resume PDF export

Resume Export saves the current draft, then offers PDF by default and JSON as an alternative.
The desktop mapper applies section, consent, and portrait visibility before calling the
independent `documents.resume` renderer. Only an included portrait is read; absent files omit
the portrait, while invalid images produce a retryable export error. The one-column A4 layout
uses the saved portrait at 35 × 45 mm on page one, collects visible contacts in a compact
header, and preserves the order of other visible sections. Bundled Tinos serif faces, navy
headings/rules, hanging bullets, and semantic inline emphasis define the print appearance.
Heading chains stay with their opening text, oversized text splits at measured line boundaries,
and each page contains only its own text. All PDF templates share a renderer lock.
File selection, temporary-file cleanup, and progress use the existing export lifecycle.
See [PDF rendering](pdf-rendering.md) for supported fields and development checks.

## Portable settings backups

`SettingsBackupUseCases` coordinates versioned JSON in `domain`, transactional reads/restores
through `SettingsBackupRepository` in `database`, and native JSON file operations in `shared`.
Imports add supported records, skip duplicates and never overwrite stored provider credentials.
Global preferences are restored in the same transaction. API keys require explicit export consent;
account caches and screen states are excluded. See [Settings backups](settings-backups.md).

## Vacancy PDF export

Vacancy Export saves edits before offering PDF by default and JSON as an alternative.
`VacancyFilesUseCases.generatePdf` uses the platform exporter and `documents.vacancy` template.
The A4 document prints job title (falling back to internal name), company, source link and
non-empty description, responsibilities, requirements and nice-to-have sections. Local keywords,
application tracking and attached/linked documents are excluded. Measured pagination preserves
searchable text, hanging bullets, section headings and page numbers. The shared export lifecycle
owns destination selection and temporary-file cleanup. See [PDF rendering](pdf-rendering.md).

## Vacancy application ZIP export

As Full Source ZIP expands the existing vacancy export dialog with a temporary file-name prefix
and checkbox lists of linked resumes and cover letters. Available records are scoped to the
vacancy's project, ordered by ID, and initially selected. The vacancy PDF is always included;
users may deselect every other document. Cancel/Escape discards the archive draft and closes
export without generating an archive. The standard save-before-export behavior is unchanged.

`VacancyArchiveUseCases` reloads and validates chosen identities before handing immutable models
to the platform archive exporter. Removed, unlinked or foreign-project selections fail before
writing. The name is converted to a bounded safe filename prefix and previewed in the form.
ZIP entries live at the archive root: `<prefix>-vacancy.pdf`, `<prefix>-resume-1.pdf`, and
`<prefix>-coverLetter-1.pdf`, with independent contiguous numbering for the selected lists.
The suggested outer filename is `<prefix>.zip`.

The desktop implementation reuses the individual PDF renderers, including resume visibility
and portrait rules. The common document-export lifecycle owns the temporary ZIP, native save
picker and cleanup on success, failure or cancellation. The form retains choices on export
errors, supports retry after loading errors, and cancels pending reads when closed. Archive
settings are not persisted and require no database changes.

## Vacancy application tracking and bulk deletion

Vacancies store an optional `appliedAt` instant as epoch milliseconds. The editor keeps the
Applied checkbox and local `YYYY-MM-DD HH:MM` text in its draft; only Save writes or clears
the timestamp. Invalid dates and nonexistent local times are rejected. Set now fills the draft
at minute precision. The list displays the local applied date before the added date.
Application tracking is local metadata and is excluded from vacancy content JSON and AI requests.
Recognition and content import preserve the current application-tracking draft.

Vacancy selection is independent of opening an editor. Confirmed bulk deletion validates every
selected identity against its project and deletes the batch in one database transaction, along
with links, screen states and unused attachment records. Linked resumes and letters are retained.
The snapshot manager removes unreferenced files only after commit, under the same lock used by
snapshot updates. Failed transactions leave both records and files intact.

Selected vacancies can be marked applied in one action. The timestamp is captured once at
the button click, and a single project-scoped SQL update changes only rows with no existing
application date. Repeated actions preserve previous application dates. This action saves
immediately; errors retain the selection for retry.

The vacancy-list footer reuses the dataset visibility-filter icon to hide or show rows with
`appliedAt`. All rows are visible initially. Hidden rows are removed from selection, and Select
all operates on visible rows only. Marking rows applied while filtered clears their selection
as they disappear. The filter remains available for an empty filtered list and is retained
while the list component is open, including after bulk deletion. No schema change is required.

## Generic provider connections and reusable operations

`ProviderConnection` contains common identity, active state, typed configuration and optional
cached information. Room stores configuration and cache in separate JSON columns. OpenRouter
credentials/model selection and Apify tokens use separate versioned configuration models.
Unsupported or malformed configuration remains stored but cannot be used. Credential values
are excluded from diagnostic `toString` output.

Settings supports OpenRouter and Apify connections. Saved connections cannot change provider
type. Apify has only a token field; AI dialogs only offer OpenRouter connections. Model discovery,
account refresh and post-request balance updates remain OpenRouter operations.

`ConnectionRunner` is a reusable operation linked to a connection by foreign key, with its own
name, active flag, operation type and JSON parameters. `ConnectionRun` is a concrete execution
with an immutable non-secret parameter snapshot, external run ID, state, timestamps and optional
error. Deleting a connection cascades to runners and their history; deleting a runner removes
its history. Use cases validate the provider/operation combination and reject execution for
disabled connections, disabled runners or invalid configuration.

The initial operation contract is LinkedIn job search through the supported Apify Actor.
Saved runners support explicit remote execution, status polling and paged result review.
Supported source results can be explicitly converted into project vacancies. Settings backup version 2 nests
runners under connections, remaps their ownership on import, and excludes execution history.

## Runner settings UI

Saved Apify connections expose a Runners section. New connections must be saved before
operations can be attached. Each runner opens a nested page scoped to its connection;
returning preserves unsaved provider settings. Supported operations include LinkedIn Jobs Scraper,
configured with HTTPS LinkedIn jobs search URLs (one per line) and a positive per-search
job limit. Duplicate URLs and surrounding whitespace are removed on Save. Runner names
are optional; unnamed entries use the operation title.

Runner active state is part of the draft and takes effect on Save. Save retains the editor
and its persisted identity, with temporary success feedback. Closing discards draft changes.
Deletion requires confirmation without typing a keyword and removes the runner's local
history. It neither deletes saved vacancies nor stops remote jobs. Storage failures retain
inputs and selection for retry. Settings does not execute an Actor or create execution
history when adding or editing a runner. The provider's own Save is independent of runner
Save; neither operation implicitly commits the other's draft. No schema change is required.


## AMS job search runners

AMS Austria Job Scraper is a second operation on existing Apify connections, using
`blackfalcondata/ams-austria-job-scraper`. New runners select LinkedIn or AMS; a saved runner's
operation is fixed so existing run history is interpreted consistently. Copy from runner offers
all supported, decodable operations and copies the selected source operation into the new draft. Runner names remain optional.

AMS settings include a required search term, location, radius (0/5/10/20/50/75/100 km),
maximum results (1–10000), inclusion of external portals, and an optional last-week/month
update filter. A positive radius requires a location. Requests explicitly use full mode,
newest-first ordering and complete plain-text descriptions, without remote incremental state.
The raw dataset is displayed with the existing dynamic columns, pagination, review filters,
AI relevance assessment and explicit project import. Similar postings with different AMS IDs
remain separate rows; no fuzzy duplicate detection is performed.

Raw AMS rows use source `AMS` and `referenceId` for exact history matching, separately from
LinkedIn IDs. Unknown fields and malformed rows remain in raw snapshots. Import maps title,
employer and description, decodes named/numeric HTML entities, and prefers a public HTTP(S)
external URL over the AMS portal URL. Missing structured requirements are not inferred.

The existing string operation and JSON configuration columns store AMS settings without any
schema change, migration or database reset. LinkedIn settings and existing history remain intact.
Settings backups include AMS runners through the same operation/configuration representation.


## Apify runner execution

The runner editor opens a separate Runs and results page. Starting uses the saved settings,
not unsaved editor inputs, and requires an active Apify connection and active valid runner.
`RunnerExecutionUseCases` coordinates local run history with `RunnerExecutionRepository`;
`ApifyExecutionRepository` adapts that contract through the shared Ktor client.

A local Pending record with an immutable, non-secret configuration snapshot is saved before
one POST to the selected supported Actor. LinkedIn input maps the per-search limit to `limitPerSource`.
The returned external ID and state are persisted before polling. Failed or cancelled starts
are never automatically retried: the user must check Apify Console because an ambiguous
network failure may still have started paid work. A known external ID can be reopened to
resume GET requests without another POST. Pending/running status is polled every five seconds while that
run is visible. Reopening a completed run with cached results reads the local database. Leaving cancels local requests, not the remote job; local deletion also does
not abort remote work.

Successful runs initially load 50 raw dataset rows. Further downloads are explicit and resume
from the persisted raw offset. The next offset and total follow provider pagination,
independently of malformed or duplicate rows skipped by the mapper. Local review pages contain
25 saved snapshots and never contact the provider.
Normalized candidates retain source IDs, title, company, job description, canonical LinkedIn
URL, location, posting date, salary, employment type and seniority when available. Missing
optional values remain empty; malformed records do not discard valid neighbors. Structured
requirements are not inferred, and the posting date is not used as the local creation date.
Source snapshots remain separate from workspace vacancies until explicit conversion.
Original dataset rows are persisted locally as described below.

Apify requests and complete text responses use `apify-http.log` beside the database and the
`ApifyHttp` console logger. Authentication headers and API tokens are redacted. The same
rotation policy as the other logs applies. Tests use mocked HTTP and fictional job postings;
verification never launches paid Actors.


## Runner result snapshots and repeat detection

`RunnerResult` / `RunnerResultEntity` stores one immutable source snapshot: run ID, raw dataset
position, source, optional external ID, original JSON object, receipt timestamp, review status
(`New`, `Imported`, `Dismissed`, `Restored`) and an optional imported vacancy reference. JSON retains all
provider fields, including unknown fields; whitespace formatting is not retained. Rows that
cannot be mapped into a candidate are still saved, even when no source ID can be extracted.

Successful dataset reads persist snapshots before returning the page to the component. Unique
indexes on `(run_id, position)` and `(run_id, source, external_id)` make repeated reads idempotent.
The first snapshot, receipt timestamp and review decision remain unchanged. A repeated known
source ID within one run keeps the first occurrence; rows without IDs use their raw positions.
Pagination continues to count raw provider rows, independently of stored or mapped row counts.

An index on `(source, external_id)` finds earlier occurrences across runners and connections.
Source means the original publisher (`LinkedIn`), independently of the execution provider
(`Apify`). A recorded import in any run takes priority, including when reviewing older runs; otherwise the latest explicit earlier review
decision wins over unreviewed occurrences. Restored is an explicit decision distinct from New,
so an old dismissal cannot reappear after restoration or another unreviewed run. Different sources and missing external IDs are not matched. The pinned action column shows previous encounters and review state in icon tooltips,
including an imported vacancy ID when still available.
Repeat detection does not overwrite vacancies or merge modified descriptions between runs.

Deleting a vacancy or its project sets the snapshot's vacancy reference to null while retaining
`Imported` as historical information. Deleting a runner or connection cascades through its runs
and snapshots. Results are excluded from settings backups together with execution history.
The first database schema now includes `runner_results`; migrations are not provided.


## Generic runner dataset table

Runner result columns are the union of top-level JSON keys seen across downloaded pages.
Column order is retained in the run alongside dataset ID, next raw offset, known total,
completion flag and cost. Incoming rows and the download cursor are committed atomically;
failed downloads can be retried without skipping rows. Status refreshes preserve this cursor.
Settings backups continue to exclude runs and results.

The table renders every saved row, including output that cannot yet be converted into a
vacancy. Missing keys are blank; nested arrays/objects use compact JSON. Cells show at most
three lines with ellipsis, and a click opens their complete selectable value. HTTP/HTTPS URLs
and addresses starting with www. are underlined clickable links, including URLs embedded in
text or JSON. Bare www. addresses open with HTTPS; the displayed source text is unchanged.
Opening a link uses the owning screen's browser handler; the original text stays selectable. Column widths
fit measured headers and current-page values within 100–320 dp; headers and cells share the
same widths. Headers stay
fixed vertically and the action and relevance columns stay fixed horizontally. Both axes have draggable
scrollbars and end buttons. Next/Previous browse saved rows; Load more performs only the
next dataset GET and does not start a paid Actor run. Cached review and rejection actions work
with no active provider or network connection.

Two independent icon toggles hide dismissed results and imported vacancies. Both categories
are visible initially. Filtering happens in SQL before local LIMIT/OFFSET pagination and uses
the same effective review status as action buttons and duplicate indicators. Explicit current
review decisions override older dismissals; a recorded import remains protected across runs. Saved/downloaded totals remain separate from visible counts.
Changing a filter returns to page one; review/import actions clamp the current page when its
last row disappears. Empty filtered pages retain filters and Load more. Remote download cursors
never depend on visibility, and toggles do not perform network requests or start paid runs.
Filter choices remain in the open runner component; no database schema change is needed.

Unreviewed or explicitly restored rows offer conversion and dismissal. A remembered dismissal
offers Restore; a remembered import offers neither conversion nor dismissal. Conversion
asks for a leaf project and uses a provider-specific local snapshot converter. The first
converter supports LinkedIn output. Vacancy creation and the result's Imported state/link are
one Room transaction, so unsupported data or an invalid project cannot leave an orphan vacancy.
Parent projects cannot receive workspace documents. Current imported results cannot be imported
again or dismissed, including repeat occurrences with a remembered import. UI, use cases and
transactional database writes enforce the same rule; raw JSON stays unchanged.

The first schema includes dataset cursor metadata; no migrations are provided.


## Prompt-based runner relevance

The fixed Relevance column on Runs and results opens the shared AI request dialog for one
saved dataset row. Active OpenRouter connections and active Vacancies/general prompts are
available. The original row is sent as a named `runner-result.json` text document; no project,
resume, cover letter or external source file is required. Provider requests and responses use
the existing HTTP traffic logger. The global application ScreenState remembers the last
prompt selected for this operation; request-local edits do not modify saved prompts.

The [runner relevance response schema](schemas/runner-relevance-v1.schema.json) accepts only
`{"score": <integer 0..100>}`. The score expresses relevance to the chosen prompt criteria,
not resume suitability, ATS acceptance or a hiring decision. The Android search example in
`prompt_examples.md` is documentation only and is never inserted automatically.

`AssessRunnerResultUseCase` validates row ownership and the active connection, calls the
remote relevance repository, and atomically updates only `relevance_score` and
`relevance_prompt` on that row. The prompt is the exact instruction sent for this assessment,
including unsaved edits. The original payload, import/rejection status and linked vacancy
are preserved. Re-fetching a dataset page cannot overwrite these local fields. The first
Room schema includes both nullable columns; no migration is provided. Settings backups
continue to exclude runner output and relevance assessments.

Null means unassessed and renders as an em dash; zero is a completed assessment and renders
as 0%. For a matching source/external ID without its own assessment, reads reuse the latest
earlier assessed occurrence and its exact prompt, independently of review-decision priority.
Zero is retained; unknown IDs and different sources never share assessments. Inherited feedback
is not copied into the new row and requires no AI request. An assessment on the current row
always takes precedence. Clicking any percentage shows the matching saved prompt. A successful
reassessment replaces only the current row's pair. Errors
keep the shared dialog open for retry, and failed writes leave the previous assessment intact.
Closing cancels the request; cancelled or late responses cannot write scores. Dataset polling
pauses while the request dialog is open and resumes afterward for pending active runs.

## Dashboard runner shortcut

The dashboard footer opens Search runners, a live picker of saved runners across connections.
Each choice shows its runner name (or operation title), connection name (or provider title),
and availability. Disabled or unconfigured runners remain visible for cached history review;
starting a new run is disabled. Empty and failed lists explain setup or offer Retry.

Add runner replaces the picker with a live selection of saved Apify connections, including
disabled connections that can still own drafts. Selecting one opens the existing runner editor.
Saving stays in the editor; Close/Back returns to the dashboard. Creating or saving a runner
never starts a remote run. If no Apify connection exists, the dialog points to API Providers.
Closing either picker creates no records; an unsaved runner draft is discarded on Back.

New runner editors offer Copy from runner. The dialog lists saved Apify runners with supported,
decodable configurations across all operation types, including disabled runners. The source
operation is copied along with its search settings. The user can edit the copied name, active
state, operation-specific search fields and result limit before Use settings. Only draft fields are applied;
the target connection, new identity and empty execution history are preserved. Cancel/Escape
discards the dialog edits. Save remains the only operation that creates the new runner.

Selecting a runner opens its existing Runs and results component as a nested dashboard page,
without starting remote work or opening its settings. Back/Escape walks from results to run
history, then back to the same dashboard. Navigation is blocked during paid starts and imports.
Local reads stop when the page loses its active lifecycle and pending polling resumes on return.

The dashboard project is supplied only as the preferred import destination. The import dialog
preselects it if it is still eligible, and the user can choose another project. Runner settings,
execution ownership, and search results remain global. No schema change is required.

Runs and results offers Settings beside Start run. It opens the selected saved runner directly
in the existing runner editor. Close/Back/Escape returns to the retained results or history page.
Local polling pauses during settings; returning reloads the runner configuration and availability
and resumes pending reads when enabled. Paid starts and imports block this shortcut as well as
other navigation. A deleted runner cannot be reopened; read failures remain retryable.
