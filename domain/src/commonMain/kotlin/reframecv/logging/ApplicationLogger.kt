package reframecv.logging

/** Defines how platforms report operation failures with their original exceptions. */
fun interface ApplicationLogger {
    fun error(operation: String, error: Throwable, context: Map<String, Any?>)

    companion object {
        val None = ApplicationLogger { _, _, _ -> }
    }
}
