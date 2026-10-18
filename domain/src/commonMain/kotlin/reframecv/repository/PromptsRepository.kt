package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent

/** Stores editable instructions and exposes only enabled prompts to their application features. */
interface PromptsRepository {
    fun observeAll(): Flow<List<Prompt>>
    fun observeAvailable(component: PromptComponent): Flow<List<Prompt>>
    suspend fun save(prompt: Prompt): Long
}
