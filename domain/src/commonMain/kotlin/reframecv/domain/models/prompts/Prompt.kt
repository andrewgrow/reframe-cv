package reframecv.domain.models.prompts

import reframecv.domain.DomainModel

/** Identifies the application feature where a prompt can be selected. */
enum class PromptComponent { Resume, Vacancies, CoverLetters, Projects }

/** Describes reusable AI instructions independently of their storage and provider. */
data class Prompt(
    val id: Long = 0,
    val title: String = "",
    val component: PromptComponent? = null,
    val text: String = "",
    val active: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) : DomainModel
