package reframecv.providers

import reframecv.domain.models.providers.ProviderAccountInfo

/** Retrieves spending limits and key validity without loading the model catalog. */
fun interface ProviderAccountClient {
    suspend fun accountInfo(apiKey: String): ProviderAccountInfo
}
