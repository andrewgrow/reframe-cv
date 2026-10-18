package reframecv.providers

import reframecv.domain.models.providers.ApiProviderType
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

/** Routes account refreshes to the implementation of the selected provider. */
class RemoteProviderAccountRepository(
    private val clients: Map<ApiProviderType, ProviderAccountClient>,
) : ProviderAccountRepository {
    override suspend fun load(type: ApiProviderType, apiKey: String) = (
        clients[type] ?: throw ProviderSetupException(
            ProviderSetupError.Unavailable,
        )
        ).accountInfo(apiKey)
}
