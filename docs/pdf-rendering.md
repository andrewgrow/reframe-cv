# PDF rendering

## Module boundaries

- `documents` owns print data, Compose templates, typography, and measured pagination.
  Templates live in `reframecv.documents.coverletter` and `reframecv.documents.resume`;
  `PrintLink` is shared print data.
  The module does not depend on domain models, the database, screen state, or native file pickers.
- `shared` maps saved domain content to print data. `DesktopCoverLetterPdfExporter` calls the
  renderer through `CoverLetterFilesUseCases`; `ApplicationDependencies` supplies the implementation.
- `DesktopDocumentExport` owns temporary JSON/PDF files, destination selection, copying, and
  cleanup. Rendering and copying run on a background dispatcher; the native picker runs on Swing.

The current renderer targets desktop JVM. Platform-specific rendering belongs behind the
same file-operation contracts when other application targets are added.

## Cover-letter export

Export saves current edits and opens a format selector with PDF first and selected by default.
Download generates the PDF and opens the native save dialog. The editor stays open. Cancelling
file selection keeps the saved letter, without reporting that a PDF was saved. Errors remain
in the export dialog for retry; temporary files are removed after success, failure, or cancellation.

Suggested names are `cover-letter-<id>-<yyyyMMdd-HHmm-ss>.pdf`. PDFs are user-selected exports,
not managed attachments or database records. Only letter body text is printed; internal name,
keywords, local IDs, and source-resume metadata are excluded. Save a newly created letter before
opening Export.

## Resume export

Resume Export also defaults to PDF, with JSON as the second option. Current changes, including
portrait changes, are saved before opening the dialog. `ResumeFilesUseCases.generatePdf` uses
`DesktopResumePdfExporter` and the same destination/temporary-file lifecycle as letters.
Suggested names are `resume-<id>-<yyyyMMdd-HHmm-ss>.pdf`.

The mapper filters every section's `visible` flag and `consent.visible` before rendering.
The portrait is read only when `photoVisible` is true and a path exists. A missing photo file
produces a text-only header; an unreadable file reports an export failure. The saved 35 × 45 mm
portrait is placed at that physical size in the first-page header, without repeating on later pages.
Internal resume name, keywords, main-resume selection, and content-language metadata are not printed.
JSON export retains its existing content contract and does not acquire photograph bytes.

The one-column A4 template uses bundled Tinos 1.340 serif faces, a 28 pt navy name,
14 pt navy headings, 10.5 pt body text with 12.2 pt line height, and thin navy rules.
Margins are 85 pt left, 42 pt right, and approximately 57 pt top/bottom. These physical
print values do not follow application theme or UI scale.

Visible contact methods are collected into a compact, wrapping header strip. Location,
authorization, and work arrangement share a line, in that order. The professional headline
follows within the text column of the header, above the first section divider; its width
excludes the portrait and the gap beside it. Remaining visible sections retain their stored order,
including custom headings, position descriptions, achievements, project details and links,
education notes, skill groups, and languages. A summary's opening paragraph is emphasized and separated from following paragraphs;
custom summary titles remain visible when supplied. Experience links have extra space above
them, and consent has a larger gap to distinguish it from the final section. Experience prints period/company first,
then position and italic location. Achievements use hanging bullets; skill category labels are
emphasized without bolding the whole list. Link captions use their labels with the original
URI attached. Dates retain year/month precision; ongoing periods
use `Present`. Consent is printed last when enabled. Print links allow HTTP(S), mailto, and tel;
other values remain plain text.

`ResumePaginator` measures styled blocks, reserves first-page header and footer space, keeps
heading chains with the opening body line, and splits oversized text at measured line boundaries.
All templates share one renderer lock because compose2pdf has process-wide rendering state.
The template supports semantic inline emphasis, hanging lists, links, and a prepared portrait;
free-form rich-text editing and arbitrary page
layouts remain separate work.

## Templates and pagination

`CoverLetterPdfTemplate` uses Compose text blocks and explicit `PdfLink` annotations. The current
layout is A4 with approximately 20 mm margins, embedded Inter at 11 pt, 16 pt line height,
paragraph spacing, and page numbers for multipage letters. Print styling is independent of the
application theme and UI scale. Plain-text URLs are not automatically converted into links;
labelled links must be supplied through print data.

`CoverLetterPaginator` measures text using the same font, width, density, and typography as the
template. Paragraphs that fit on a page stay together. Oversized paragraphs split at measured line
boundaries, and fragments are measured again before placement. Footer space is reserved.
Each page is rendered separately through compose2pdf's public `renderToPdf(pages = ...)` API.
This keeps off-page text out of the PDF text layer and prevents repeated text during extraction.
Do not replace it with clipping a full document onto each page.

Rendering is serialized because compose2pdf's renderer is not thread-safe. Contextual and
discretionary glyph substitutions are disabled to preserve Unicode through the SVG conversion.
The renderer produces vector text with embedded fonts, rather than page-sized raster images.

New printable block types must participate in measurement and rendering together. Current
pagination handles text and labelled links; arbitrary layouts, images, tables, rich text, and
PDF accessibility tags require separate implementation and validation.

## Development checks

Run renderer tests independently:

```shell
./gradlew :documents:jvmTest --no-parallel
```

Run document component, export, and screenshot tests:

```shell
./gradlew :shared:jvmTest --tests '*CoverLetter*' --tests '*Resume*' --no-parallel
```

Renderer fixtures live in test source sets.
`DesktopResumePdfExporterTest` writes a fictional review PDF to
`shared/build/reports/pdf/resume.pdf`, including a synthetic portrait, and
`shared/build/reports/pdf/resume-text.pdf` for a compact text-only layout. Tests check complete text in reading order,
no missing or repeated content across pages, embedded fonts, Unicode and punctuation, print
margins, long paragraphs, explicit newlines, and link URI annotations. Shared tests cover saving
current edits before export, format selection, progress, retry, cancellation, and temporary-file
cleanup. Use the normal golden-recording workflow from the README for intentional UI changes.

For template changes, also export a short and a multipage letter from the application. Render
all pages for visual inspection and check selection/copying and links in target PDF viewers.
Text-extraction tests alone cannot establish visual correctness.

## Licenses and packaging

The renderer uses compose2pdf (Apache-2.0), Apache PDFBox (Apache-2.0), and bundled Inter fonts
(SIL Open Font License 1.1), plus bundled Tinos faces under the same font license. See [Third-Party Notices](../THIRD_PARTY_NOTICES.md).
Complete PDF-related license and notice texts are stored under
[`desktopApp/src/main/resources/licenses/pdf`](../desktopApp/src/main/resources/licenses/pdf/).
Gradle packages these resources in the desktop application JAR, which is included in native
distributions. Verify resource inclusion with `./gradlew :desktopApp:jar` when changing packaging.
Keep the texts and their source references current when upgrading PDF dependencies or fonts.
