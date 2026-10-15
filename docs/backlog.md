# Backlog and open questions

Deferred ideas, not a release plan or a required sequence. Choose work as needs arise,
agree on its scope before implementation, and remove completed or abandoned items.
Current behavior belongs in [architecture and constraints](architecture.md); commands
belong in the [README](../README.md).

## Resume adaptations

- [ ] Decide where adaptation starts: a vacancy, the resume list, or both.
- [ ] Default to the main resume, with an explicit alternative source.
- [ ] Create an independent document linked by `sourceResumeId`; decide what is copied
  and when to assign the vacancy's `resumeId`.
- [ ] Clarify whether explicit version history is needed beyond independent adaptations.

## Cover letters

- [ ] Agree on list columns, editor fields, and validation.
- [ ] Add project-scoped browsing, editing, creation, and confirmed deletion.
- [ ] Define selecting/clearing an existing letter for a vacancy.

## Vacancy sources

- [ ] Define URL-to-snapshot fetching, supported formats, and failure/cancellation behavior.
- [ ] Define source parsing separately from fetching, including supported inputs and mapping.
- [ ] Preserve immutable snapshots independently of editable vacancy fields.

## Print and PDF export

- [ ] Choose the rendering/template approach and initial output layout.
- [ ] Honor section, consent, and photo visibility; use the prepared portrait for printing.

## UI and search

- [ ] Expose UI scale controls and persist the selected scale in application settings.
- [ ] Define multi-keyword search semantics: match all or any.
- [ ] Revisit left-pane sizing and deep nesting if current scrolling is insufficient.
