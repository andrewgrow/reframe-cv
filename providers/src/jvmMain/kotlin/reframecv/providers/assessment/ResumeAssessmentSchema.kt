package reframecv.providers.assessment

/** Loads the response contract packaged for resume–vacancy assessment requests. */
fun resumeAssessmentSchema(): String = checkNotNull(
    RemoteResumeAssessmentRepository::class.java.getResource("/resume-assessment-v1.schema.json"),
).readText()
