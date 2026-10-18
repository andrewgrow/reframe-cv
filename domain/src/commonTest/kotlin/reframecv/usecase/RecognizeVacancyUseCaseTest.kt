package reframecv.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.attachments.SelectedAttachment
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionFileException
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognitionSource
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository

class RecognizeVacancyUseCaseTest {
    private val connection = reframecv.domain.models.providers.ProviderConnection(
        id = 1,
        type = reframecv.domain.models.providers.ApiProviderType.OpenRouter,
        name = "Test",
        apiKey = "test-key",
        modelId = "test-model",
    )
    private val settings = ProviderSettingsUseCases(
        object : reframecv.repository.ProviderConnectionsRepository {
            override fun observe() = kotlinx.coroutines.flow.flowOf(listOf(connection))
            override suspend fun get(id: Long) = connection.takeIf { it.id == id }
            override suspend fun save(
                connection: reframecv.domain.models.providers.ProviderConnection,
            ) = connection.id
            override suspend fun setActive(id: Long, active: Boolean) = Unit
            override suspend fun updateAccountInfo(
                id: Long,
                type: reframecv.domain.models.providers.ApiProviderType,
                apiKey: String,
                info: reframecv.domain.models.providers.ProviderAccountInfo,
            ) = Unit
            override suspend fun updateCreditBalance(
                id: Long,
                type: reframecv.domain.models.providers.ApiProviderType,
                managementApiKey: String,
                balance: reframecv.domain.models.providers.ProviderCreditBalance,
            ) = Unit
        },
        reframecv.repository.ProviderModelsRepository { _, _ -> emptyList() },
        reframecv.repository.ProviderAccountRepository { _, _ -> error("No account request") },
    )
    private val source = RecognitionSource.Selected(
        SelectedAttachment("/private/job.pdf", "job.pdf"),
    )
    private val document = RecognitionDocument("job.pdf", "%PDF-1.7".encodeToByteArray())

    @Test
    fun preparesTheSourceBeforePassingOnlyTheDocumentToTheRepository() = runBlocking {
        val order = mutableListOf<String>()
        val result = RecognitionOutcome.Failed(RecognitionError.NotConfigured)
        val action = RecognizeVacancyUseCase(
            VacancyDocumentReader {
                assertSame(source, it)
                order += "read"
                document
            },
            repository {
                assertSame(document, it)
                order += "recognize"
                result
            },
            settings,
        )
        assertSame(result, action(source, connection.id, "Extract"))
        assertEquals(listOf("read", "recognize"), order)
    }

    @Test
    fun unreadableDocumentsNeverReachTheExternalRepository() = runBlocking {
        val failure = RecognitionFileException(RecognitionError.UnreadableFile)
        val action = RecognizeVacancyUseCase(
            VacancyDocumentReader { throw failure },
            repository { error("Must not call the provider") },
            settings,
        )
        assertSame(
            failure,
            assertFailsWith<RecognitionFileException> {
                action(source, connection.id, "Extract")
            },
        )
    }

    @Test
    fun cancellationDuringPreparationPreventsAnExternalRequestEvenIfTheReaderReturnsLate() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val action = RecognizeVacancyUseCase(
                VacancyDocumentReader {
                    started.complete(Unit)
                    withContext(NonCancellable) { release.await() }
                    document
                },
                repository { error("Cancelled operation must not reach the provider") },
                settings,
            )
            val job = launch { action(source, connection.id, "Extract") }
            withTimeout(10_000) {
                started.await()
                job.cancel()
                release.complete(Unit)
                job.cancelAndJoin()
            }
        }

    private fun repository(action: suspend (RecognitionDocument) -> RecognitionOutcome) =
        object : VacancyRecognitionRepository {
            override suspend fun recognize(
                document: RecognitionDocument,
                connection: reframecv.domain.models.providers.ProviderConnection,
                instruction: String,
            ) = action(document)
        }
}
