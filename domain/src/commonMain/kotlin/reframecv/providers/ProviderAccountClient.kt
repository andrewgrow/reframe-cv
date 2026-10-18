package reframecv.providers

import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Retrieves key budgets and optional account credits without loading the model catalog. */
fun interface ProviderAccountClient {
    suspend fun accountInfo(apiKey: String): ProviderAccountInfo
    suspend fun balance(managementApiKey: String): ProviderCreditBalance =
        throw ProviderSetupException(ProviderSetupError.Unavailable)
}
