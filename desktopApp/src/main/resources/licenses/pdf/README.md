# PDF dependency licenses

These files are packaged as desktop application resources under `licenses/pdf/`.

| File | Source |
| --- | --- |
| `compose2pdf-LICENSE.txt` | compose2pdf Apache-2.0 license, retrieved from https://github.com/chrisjenx/compose2pdf/blob/main/LICENSE |
| `PDFBox-LICENSE.txt` | Unmodified `META-INF/LICENSE` from `org.apache.pdfbox:pdfbox:3.0.7` |
| `PDFBox-NOTICE.txt` | Unmodified `META-INF/NOTICE` from the same PDFBox artifact |
| `Inter-LICENSE.txt` | Inter copyright and SIL OFL 1.1, retrieved from https://github.com/rsms/inter/blob/master/LICENSE.txt |

The renderer uses the unmodified Inter fonts bundled by compose2pdf 1.4.0. PDFBox's license
file includes the dependency notices shipped with that artifact. These files supplement
license and notice resources already present in dependency JARs; keep those resources intact.
