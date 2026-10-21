# Backlog and open questions

Deferred ideas, not a release plan or a required sequence. Choose work as needs arise,
agree on its scope before implementation, and remove completed or abandoned items.
Current behavior belongs in [architecture and constraints](architecture.md); commands
belong in the [README](../README.md).

## Resume adaptations

AI improvement is implemented through the shared AI Request Dialog, opened from the icon
beside each linked resume's match score. Each successful request creates an independent
resume in the same project and adds a vacancy link; its `sourceResumeId` identifies the
selected source. Users can assess that version and repeat improvement manually. There is
no separate improvement page or automatic score/improve loop.

Remaining questions and ideas:

- [ ] Consider a comparison view between source and adaptation before further editing.
- [ ] Decide whether provenance links are sufficient or explicit version browsing is needed.
- [ ] Decide how to compare scores across provider/model/prompt/vacancy changes; scores
  from different setups are not necessarily comparable.

The divo-resume backend was a reference for whole-document generation. The improvement
example prompt permits plausible experience additions within existing positions/projects,
integrated directly into the text for user review, without separate assumption explanations.
Identity, employers, position titles, dates, qualifications, and other fixed facts are preserved.

## Cover letters

Vacancies now support existing-letter selection, draft unlinking, AI generation from a selected
linked resume, letter editing and confirmed permanent deletion. Letters remain reusable project documents.

- [ ] Add a project-wide cover-letter list accessible from the dashboard.
- [ ] Add manual letter creation independently of a vacancy.
- [ ] Decide on letter export and printing formats.

## Vacancy sources

- [ ] Decide whether recognition should offer a preview/diff before replacing draft fields.
- [ ] Define URL-to-snapshot fetching, supported formats, and failure/cancellation behavior.
- [ ] Consider parsing additional source formats after the PDF integration.

## Print and PDF export

- [ ] Choose the rendering/template approach and initial output layout.
- [ ] Honor section, consent, and photo visibility; use the prepared portrait for printing.

### Resume reference: adaptation

Reviewed the two-page `Adopted_MobileAppSeniorDeveloper.pdf` supplied
by the user against the current domain models, JSON schema, and editor. The PDF is a
visual/content reference, not a new required schema. The reference is outside the repository;
the findings below should remain understandable without access to the original file.

Coverage after the model/schema/editor update:

| Reference content | Current representation | Limitation |
| --- | --- | --- |
| Name, target headline, location | `ResumeProfile` | Supported. |
| Phone, email, LinkedIn | `ResumeContactsSection` entries with type, label, value | Supported; printing must support placing contacts in the header. |
| Professional summary | `ResumeSummarySection.text` | Supported as plain text, without inline emphasis. |
| Position, employer, location, year-only and ongoing periods | `ResumeExperience` and `ResumeDateRange` | Supported. |
| Introductory paragraph about each job/company | `ResumeExperience.description` | Supported separately from achievements. |
| Key results and quantified achievements | `ResumeExperience.achievements` | Supported as ordered plain-text items. |
| Named projects and their descriptions/tools | `ResumeProject` | Supported, including labeled links. |
| Google Play/project links under a position | `ResumeExperience.links` and `ResumeProject.links` | Ordered labeled URLs scoped to their owner. |
| Categorized professional skills | `ResumeSkillsSection.groups` | Supported. |
| Institution, degree, specialty, graduation year | `ResumeEducation` | Supported; bachelor's and master's degrees can be separate entries with distinct dates. |
| Graduated with honors | `ResumeEducation.notes` | Supported separately from degree and specialty. |
| Languages and proficiency | `ResumeLanguagesSection` | Supported. |
| Remote/hybrid preference and work authorization | `ResumeProfile.workArrangement` and `workAuthorization` | Separate optional free-text fields. |

The inspected Kotlin content fields and their corresponding JSON Schema definitions agree;
the content gaps identified in this reference have been implemented in both contracts. Photo storage
and visibility already exist outside portable content JSON by design. Rich text, line rules,
contact placement, and pagination are rendering concerns, not missing resume facts.

Implemented content additions:

- Each experience entry has an optional introductory description and labeled links.
- Projects have their own labeled links, preserving their relationship to the project.
- Education entries have optional notes for honors and other qualification details.
- Profile has separate optional work arrangement and work authorization fields.
- Model, JSON schema, import/export, and editor share the same fields; older documents
  use empty defaults. Round-trip samples cover the extended reference structure.

Remaining presentation work:

- [ ] Use a single-column print template with a compact name/contact/location header,
  a clear target headline, and a short summary near the top.
- [ ] Structure each position as identity and period, a short context paragraph, then
  achievement bullets and relevant project links. Keep quantified outcomes easy to scan.
- [ ] Render skills in compact labeled groups with emphasis on group labels rather than
  making the entire skills block bold.
- [ ] Use consistent spacing and restrained separators between sections/positions;
  preserve section order and custom headings from the content model.
- [ ] Handle pagination deliberately: keep position headings with their opening content,
  avoid orphan headings, and retain readable font sizes instead of forcing two pages.
- [ ] Verify the rendered PDF visually and check extracted text order, contacts, links,
  dates, and bullets. Keep template styling separate from editable factual content.

## UI and search

- [ ] Expose UI scale controls and persist the selected scale in application settings.
- [ ] Define multi-keyword search semantics: match all or any.
- [ ] Revisit left-pane sizing and deep nesting if current scrolling is insufficient.

## API providers

- [ ] Add provider connection deletion and decide how to handle deleting a connection in use.
- [ ] Add API-key rotation controls to the provider Management section.

## AI requests

- [ ] Consider request history and reuse of parsed file annotations.
