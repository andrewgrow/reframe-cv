package reframecv.providers.relevance

/** Loads the packaged response contract for prompt-based dataset relevance. */
fun runnerRelevanceSchema(): String = checkNotNull(
    RemoteRunnerRelevanceRepository::class.java.getResource("/runner-relevance-v1.schema.json"),
).readText()
