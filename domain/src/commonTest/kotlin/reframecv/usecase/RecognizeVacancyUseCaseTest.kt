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
        )
        assertSame(result, action(source))
        assertEquals(listOf("read", "recognize"), order)
    }

    @Test
    fun unreadableDocumentsNeverReachTheExternalRepository() = runBlocking {
        val failure = RecognitionFileException(RecognitionError.UnreadableFile)
        val action = RecognizeVacancyUseCase(
            VacancyDocumentReader { throw failure },
            repository { error("Must not call the provider") },
        )
        assertSame(failure, assertFailsWith<RecognitionFileException> { action(source) })
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
            )
            val job = launch { action(source) }
            withTimeout(10_000) {
                started.await()
                job.cancel()
                release.complete(Unit)
                job.cancelAndJoin()
            }
        }

    private fun repository(action: suspend (RecognitionDocument) -> RecognitionOutcome) =
        object : VacancyRecognitionRepository {
            override suspend fun recognize(document: RecognitionDocument) = action(document)
        }
}
