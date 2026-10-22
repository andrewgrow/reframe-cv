package reframecv.domain.models.providers

import kotlinx.serialization.json.Json

/** Encodes typed provider settings and preserves unsupported records for later repair. */
object ProviderConfigurationJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    fun decode(type: ApiProviderType, raw: String): ProviderConfiguration = try {
        when (type) {
            ApiProviderType.OpenRouter -> json.decodeFromString<OpenRouterConfiguration>(raw)
                .also { require(it.version == 1) }

            ApiProviderType.Apify -> json.decodeFromString<ApifyConfiguration>(raw)
                .also { require(it.version == 1) }
        }
    } catch (_: IllegalArgumentException) {
        InvalidProviderConfiguration(raw)
    }
    fun encode(configuration: ProviderConfiguration): String = when (configuration) {
        is OpenRouterConfiguration -> json.encodeToString(configuration)
        is ApifyConfiguration -> json.encodeToString(configuration)
        is InvalidProviderConfiguration -> configuration.raw
    }
    fun portable(configuration: ProviderConfiguration, secrets: Boolean): ProviderConfiguration =
        when (configuration) {
            is OpenRouterConfiguration -> if (secrets) {
                configuration
            } else {
                configuration.copy(apiKey = "", managementApiKey = "")
            }

            is ApifyConfiguration -> if (secrets) {
                configuration
            } else {
                configuration.copy(
                    apiToken = "",
                )
            }

            is InvalidProviderConfiguration -> InvalidProviderConfiguration("{}")
        }
    fun decodeCache(type: ApiProviderType, raw: String?): ProviderCachedInfo? = try {
        if (type == ApiProviderType.OpenRouter && raw != null) {
            json.decodeFromString<OpenRouterCachedInfo>(raw)
        } else {
            null
        }
    } catch (_: IllegalArgumentException) {
        null
    }
    fun encodeCache(cache: ProviderCachedInfo?): String? = when (cache) {
        is OpenRouterCachedInfo -> json.encodeToString(cache)
        null -> null
    }
}
