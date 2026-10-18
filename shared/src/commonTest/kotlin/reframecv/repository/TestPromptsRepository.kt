package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent

class TestPromptsRepository : PromptsRepository {
    val records = MutableStateFlow<List<Prompt>>(emptyList())
    var failSave = false
    var failLoad = false
    override fun observeAll() = records.map {
        check(!failLoad)
        it
    }
    override fun observeAvailable(component: PromptComponent) = records.map { prompts ->
        prompts.filter { it.active && (it.component == null || it.component == component) }
    }
    override suspend fun save(prompt: Prompt): Long {
        check(!failSave)
        val id = prompt.id.takeIf { it != 0L } ?: ((records.value.maxOfOrNull { it.id } ?: 0L) + 1)
        records.value =
            (records.value.filterNot { it.id == id } + prompt.copy(id = id)).sortedBy { it.id }
        return id
    }
}
