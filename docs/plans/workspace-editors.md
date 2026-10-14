# Plan: workspace editors and vacancy sources

## Status and scope

Active follow-up to the completed workspace/dashboard milestone, as of 2026-10-13.
The [development log](../development-log.md) preserves earlier decisions and verification.
This plan tracks remaining work; unchecked items are not implemented.

Resume browsing, creation, editing, main-resume selection, and confirmed deletion
are implemented. Main-resume adaptations are the next resume workflow. Agree on
each editor's UX and validation before implementing it.
Later sections are separate work, not prerequisites for the first resume editor.

## Existing foundation

- Projects have a tree, mode-based routing, and a three-section dashboard.
- Resume, Vacancy, and CoverLetter have domain models, Room entities, keyword tables,
  DAOs, repositories, optional relationships, and observable project-scoped lists.
- Vacancy list, full-page creation/editing, permanent deletion, and source-file
  attachment are implemented. URL fetching and source parsing are not implemented.
- Resume content is a typed document with ordered, titled, hideable sections,
  free skill categories, professional projects, and partial/ongoing dates. It is
  persisted as versioned JSON. Creation and editing support every section type;
  JSON file import fills the creation draft without saving. Optional document language and a free-text
  consent statement are editable and included in JSON exports. Consent has an
  Include in resume flag that preserves its text when switched off. Summary uses
  a fixed heading, and comma-separated item fields grow to fit wrapped/multiline text.
- The resume list opens from its dashboard card and observes project-scoped data.
  Add opens a full-page structured creation form. Only the resume name is required.
  Clicking a saved name or date opens the same form as Resume, with Save and
  permanent Delete actions. Save keeps management open and briefly shows Success;
  Close discards unsaved changes. Deletion requires typing DELETE in a confirmation dialog.
  Date fields accept an optional year or year-month and an ongoing period.
- Export saves the current resume draft before opening the dialog with as JSON
  selected. Invalid drafts and failed saves keep the dialog closed. Download generates
  schema-compatible JSON, opens the native save picker, and removes its temporary
  file on success, cancellation, or failure. Export failures remain retryable.
- Import on the creation page opens the native JSON picker and parses a temporary copy.
  Recognized fields/sections replace document content; unknown fields, unsupported
  sections, and incompatible values are skipped. Name, keywords, and main-resume
  selection stay unchanged. Save remains explicit; close or successful creation
  releases all temporary sources. Cancelled/failed imports preserve the draft.
- Project.mainResumeId and Resume.sourceResumeId exist. Main-resume assignment is
  validated by the DAO. Creation/editing can select or clear the main resume;
  document and selection changes are saved atomically. Adaptation remains planned.
- All deletion is permanent. Deleting a project removes its subtree and contents;
  shared source snapshots survive until their last vacancy reference is removed.
- Creating the first workspace record sets Workspace; deleting the last returns
  Unconfigured. Opening Configure alone does not persist a mode change.
- Vacancy and resume editing use nested list/editor stacks with component-owned drafts.
  Back returns editor → list → dashboard → parent project list; deletion confirmation
  is cancelled first. Project dialogs still handle Escape before page navigation.
- Project creation/editing uses the shared ResizableDialog for future dialog reuse.
- Keep the development schema at version 1 until real data requires migrations.

## 1. Resume list and editor

- [x] Agree on list columns, editor fields, and minimum name/content validation.
- [x] Open the project's resume list from its dashboard section.
- [x] Observe ResumesRepository with loading, empty, error, and Retry states.
- [x] Define typed resume content and verify versioned JSON storage and independent adaptations.
- [x] Publish the version 1 JSON Schema and test its agreement with serializers and date rules.
- [x] Add creation using every section type, editable titles/visibility, ordering, and repeated entries.
- [x] Add editing of saved resumes using the structured content model.
- [x] Import schema-based JSON files into creation drafts with temporary-file cleanup.
- [x] Add print-ready portraits, Save-based replacement/removal, and file cleanup outside JSON.
- [x] Group resume metadata responsively and separate Profile/Sections visually.
- [x] Clarify experience position/project actions and confirm nested removal.
- [x] Add permanent deletion with a typed DELETE dialog and existing relationship cleanup.
- [x] Preserve creation/editing drafts on failure, block duplicate writes, and follow existing Back behavior.
- [x] Verify creation navigation, persistence, count/mode updates, failures, and relevant goldens.
- [x] Verify editing navigation, persistence, preserved identity/provenance, failures, and goldens.
- [x] Verify deletion navigation, persistence, relationship cleanup, and goldens.

## 2. Main resume and adaptations

- [x] Expose validated main-resume assignment through the repository.
- [x] Add user-facing selection, clearing, and indication in resume creation/editing.
- [ ] Agree on how users start an adaptation from a vacancy or the resume list.
- [ ] Use the main resume as the default source, with an explicit alternative choice.
- [ ] Create an independent editable resume linked by sourceResumeId; decide which
  fields are copied and when the vacancy's resumeId is assigned.
- [ ] Verify source independence, same-project relationships, and deletion cleanup.

## 3. Cover-letter list and editor

- [ ] Agree on list columns, editor fields, and minimum name/content validation.
- [ ] Open the project's cover-letter list from its dashboard section.
- [ ] Add independent creation, editing, and confirmed permanent deletion.
- [ ] Agree on selecting an existing letter for a vacancy and clearing that relationship.
- [ ] Verify loading, Retry, persistence, live counts, relationship cleanup, and goldens.

## 4. Vacancy source workflow — separate follow-up

- [ ] Agree on the URL-to-snapshot workflow and error/cancellation behavior.
- [ ] Fetch and preserve a new immutable snapshot without overwriting earlier snapshots.
- [ ] Define parsing separately from fetching, including supported inputs and field mapping.
- [ ] Keep editable vacancy fields independent from the original snapshot.

## Remaining cross-cutting questions

- [ ] Define multiple-keyword search: match all or any.
- [ ] Revisit left-pane sizing and deep nesting in narrow windows if the current
  token-based width and horizontal scrolling are insufficient.

## Verification

For each implementation step, run checks appropriate to the change and update only
intentionally changed screenshot references. Use the current README commands for
full tests, ktlint, Detekt, and coverage verification before closing a milestone.
