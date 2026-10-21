package reframecv.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import reframecv.domain.models.coverletter.CoverLetter

/** Serializes portable letter content independently of local identity and document links. */
object CoverLetterContentJson {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(letter: CoverLetter): String = json.encodeToString(
        Document.serializer(),
        Document(1, Content(letter.name, letter.content, letter.keywords)),
    ) + "\n"

    /** Identifies the independently versioned cover-letter exchange format. */
    @Serializable
    private data class Document(val formatVersion: Int, val content: Content)

    /** Holds fields that can be copied between projects and installations. */
    @Serializable
    private data class Content(val name: String, val text: String, val keywords: List<String>)
}
