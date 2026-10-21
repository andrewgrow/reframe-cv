package reframecv.providers.coverletters

/** Loads the letter response contract shipped with the application. */
fun coverLetterGenerationSchema(): String = checkNotNull(
    RemoteCoverLetterGenerationRepository::class.java.getResource(
        "/cover-letter-generation-v1.schema.json",
    ),
).readText()
