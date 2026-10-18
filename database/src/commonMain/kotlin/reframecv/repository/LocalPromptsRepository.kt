package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.prompts.PromptEntity
import reframecv.database.prompts.PromptsDao
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.shared.nowMillis

/** Keeps user-created prompt instructions available across application launches. */
class LocalPromptsRepository(private val dao: PromptsDao) : PromptsRepository {
    override fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomainModel() } }
    override fun observeAvailable(component: PromptComponent) =
        dao.observeAvailable(component.name).map { rows -> rows.map { it.toDomainModel() } }
    override suspend fun save(prompt: Prompt): Long = dao.save(prompt.toEntity())

    private fun Prompt.toEntity(): PromptEntity {
        val now = nowMillis()
        return PromptEntity(id, title, component?.name, text, active, now, now)
    }
}
