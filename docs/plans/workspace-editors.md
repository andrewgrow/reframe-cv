# Plan: workspace editors and vacancy sources

## Status and scope

Active follow-up to the completed workspace/dashboard milestone, as of 2026-10-12.
The [development log](../development-log.md) preserves earlier decisions and verification.
This plan tracks remaining work; unchecked items are not implemented.

The proposed next step is the resume list and editor. Agree on each editor's UX and
validation before implementing it. Later sections are separate work, not prerequisites
for the first resume editor.

## Existing foundation

- Projects have a tree, mode-based routing, and a three-section dashboard.
- Resume, Vacancy, and CoverLetter have domain models, Room entities, keyword tables,
  DAOs, repositories, optional relationships, and observable project-scoped lists.
- Vacancy list, full-page creation/editing, permanent deletion, and source-file
  attachment are implemented. URL fetching and source parsing are not implemented.
- Resume content is a typed document with ordered, titled, hideable sections,
  free skill categories, professional projects, and partial/ongoing dates. It is
  persisted as versioned JSON; section editing and import are still future work.
- Project.mainResumeId and Resume.sourceResumeId exist. Main-resume assignment is
  validated by the DAO, but there is no selection UI or adaptation workflow.
- All deletion is permanent. Deleting a project removes its subtree and contents;
  shared source snapshots survive until their last vacancy reference is removed.
- Creating the first workspace record sets Workspace; deleting the last returns
  Unconfigured. Opening Configure alone does not persist a mode change.
- Vacancy editing uses a nested list/editor stack with a component-owned draft.
  Back returns editor → list → dashboard → parent project list; deletion confirmation
  is cancelled first. Project dialogs still handle Escape before page navigation.
- Project creation/editing uses the shared ResizableDialog for future dialog reuse.
- Keep the development schema at version 1 until real data requires migrations.

## 1. Resume list and editor

- [ ] Agree on list columns, editor fields, and minimum name/content validation.
- [ ] Open the project's resume list from its dashboard section.
- [ ] Observe ResumesRepository with loading, empty, error, and Retry states.
- [x] Define typed resume content and verify versioned JSON storage and independent adaptations.
- [x] Publish the version 1 JSON Schema and test its agreement with serializers and date rules.
- [ ] Add creation and editing using the structured content model.
- [ ] Add permanent deletion with confirmation and existing relationship cleanup.
- [ ] Preserve drafts on failure, block duplicate writes, and follow existing Back behavior.
- [ ] Verify navigation, persistence, count/mode updates, failures, and relevant goldens.

## 2. Main resume and adaptations

- [ ] Expose validated main-resume assignment through the repository.
- [ ] Add user-facing selection, clearing, and indication of the main resume.
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
