# PDF dependency licenses

These files are packaged as desktop application resources under `licenses/pdf/`.

| File | Source |
| --- | --- |
| `compose2pdf-LICENSE.txt` | compose2pdf Apache-2.0 license, retrieved from https://github.com/chrisjenx/compose2pdf/blob/main/LICENSE |
| `PDFBox-LICENSE.txt` | Unmodified `META-INF/LICENSE` from `org.apache.pdfbox:pdfbox:3.0.7` |
| `PDFBox-NOTICE.txt` | Unmodified `META-INF/NOTICE` from the same PDFBox artifact |
| `Tinos-LICENSE.txt` | Tinos copyright and SIL OFL 1.1 from https://github.com/googlefonts/tinos/blob/main/OFL.txt |
| `Inter-LICENSE.txt` | Inter copyright and SIL OFL 1.1, retrieved from https://github.com/rsms/inter/blob/master/LICENSE.txt |

The letter renderer uses the unmodified Inter fonts bundled by compose2pdf 1.4.0.
The resume renderer bundles unmodified static Tinos 1.340 faces from Google Fonts; their
source references and checksums are in `documents/src/jvmMain/resources/fonts/tinos/README.md`. PDFBox's license
file includes the dependency notices shipped with that artifact. These files supplement
license and notice resources already present in dependency JARs; keep those resources intact.
