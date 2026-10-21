# PDF rendering

## Module boundaries

- `documents` owns print data, Compose templates, typography, and measured pagination.
  Cover-letter code lives in `reframecv.documents.coverletter`; `PrintLink` is shared print data.
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

Run cover-letter component, export, and screenshot tests:

```shell
./gradlew :shared:jvmTest --tests '*CoverLetter*' --no-parallel
```

Renderer fixtures live only in `documents/src/jvmTest`. Tests check complete text in reading order,
no missing or repeated content across pages, embedded fonts, Unicode and punctuation, print
margins, long paragraphs, explicit newlines, and link URI annotations. Shared tests cover saving
current edits before export, format selection, progress, retry, cancellation, and temporary-file
cleanup. Use the normal golden-recording workflow from the README for intentional UI changes.

For template changes, also export a short and a multipage letter from the application. Render
all pages for visual inspection and check selection/copying and links in target PDF viewers.
Text-extraction tests alone cannot establish visual correctness.

## Licenses and packaging

The renderer uses compose2pdf (Apache-2.0), Apache PDFBox (Apache-2.0), and bundled Inter fonts
(SIL Open Font License 1.1). See [Third-Party Notices](../THIRD_PARTY_NOTICES.md).
Complete PDF-related license and notice texts are stored under
[`desktopApp/src/main/resources/licenses/pdf`](../desktopApp/src/main/resources/licenses/pdf/).
Gradle packages these resources in the desktop application JAR, which is included in native
distributions. Verify resource inclusion with `./gradlew :desktopApp:jar` when changing packaging.
Keep the texts and their source references current when upgrading PDF dependencies or fonts.
