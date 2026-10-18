package reframecv.usecase

import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.repository.PromptsRepository

/** Supplies prompt management and component-scoped prompt selection. */
class PromptsUseCases(private val repository: PromptsRepository) {
    fun observeAll() = repository.observeAll()
    fun observeAvailable(component: PromptComponent) = repository.observeAvailable(component)
    suspend fun save(prompt: Prompt): Long {
        require(prompt.text.isNotBlank()) { "Prompt text must not be blank" }
        return repository.save(prompt.copy(title = prompt.title.trim(), text = prompt.text.trim()))
    }
}
