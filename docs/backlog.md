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

- [ ] Add editing controls for responsibilities, requirements, and nice-to-have lists.
- [ ] Decide whether recognition should offer a preview/diff before replacing draft fields.
- [ ] Define URL-to-snapshot fetching, supported formats, and failure/cancellation behavior.
- [ ] Implement OpenRouter PDF requests: authentication, upload/response protocol, limits,
  compatible structured-output schema, and real request cancellation.
- [ ] Consider parsing additional source formats after the PDF integration.

## Print and PDF export

- [ ] Choose the rendering/template approach and initial output layout.
- [ ] Honor section, consent, and photo visibility; use the prepared portrait for printing.

## UI and search

- [ ] Expose UI scale controls and persist the selected scale in application settings.
- [ ] Define multi-keyword search semantics: match all or any.
- [ ] Revisit left-pane sizing and deep nesting if current scrolling is insufficient.

## API provider settings

- [ ] Add Settings navigation and a user-managed list of provider connections; start empty.
- [ ] Add connection creation/editing: supported provider type, name, credentials, key validation,
  model loading/selection, and explicit Save. Choose an active connection for AI operations.
- [ ] Extend provider contracts with credential checks and model discovery when implementing
  the settings flow; preserve drafts on failure and keep credentials out of logs.
