package reframecv.providers.recognition

/** Loads the packaged recognition contract used by external vacancy requests. */
fun vacancyRecognitionSchema(): String = checkNotNull(
    RemoteVacancyRecognitionRepository::class.java.getResource(
        "/vacancy-recognition-v1.schema.json",
    ),
).readText()
