package reframecv.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout

/** Shares cancellable request timeouts across platform engines without logging document bodies. */
fun createNetworkClient(engine: HttpClientEngine) = HttpClient(engine) {
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        socketTimeoutMillis = REQUEST_TIMEOUT_MS
    }
}

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val REQUEST_TIMEOUT_MS = 120_000L
