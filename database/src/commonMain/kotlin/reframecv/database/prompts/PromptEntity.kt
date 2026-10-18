package reframecv.database.prompts

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent

/** Represents reusable prompt instructions stored in the local database. */
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val component: String?,
    val text: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) : DatabaseModel<Prompt> {
    override fun toDomainModel() = Prompt(
        id,
        title,
        component?.let(PromptComponent::valueOf),
        text,
        active,
        createdAt,
        updatedAt,
    )
}
