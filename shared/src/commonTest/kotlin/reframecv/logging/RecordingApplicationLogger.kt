package reframecv.logging

import kotlinx.coroutines.channels.Channel

class RecordingApplicationLogger : ApplicationLogger {
    data class Failure(val operation: String, val error: Throwable, val context: Map<String, Any?>)

    val failures = Channel<Failure>(Channel.UNLIMITED)

    override fun error(operation: String, error: Throwable, context: Map<String, Any?>) {
        failures.trySend(Failure(operation, error, context))
    }
}
