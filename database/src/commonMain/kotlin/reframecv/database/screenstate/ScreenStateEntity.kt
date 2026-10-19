package reframecv.database.screenstate

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.database.DatabaseModelFactory
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType

/** Represents a screen's stored JSON view state for one document or standalone page. */
@Entity(
    tableName = "screen_states",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["project_id"],
        ), Index(value = ["screen", "document_id"], unique = true),
    ],
)
data class ScreenStateEntity(
    @PrimaryKey val key: String,
    val screen: String,
    @ColumnInfo(name = "document_id") val documentId: Long?,
    val states: String,
    @ColumnInfo(name = "project_id") val projectId: Long? = null,
) : DatabaseModel<ScreenState> {
    override fun toDomainModel() = ScreenState(
        ScreenStateOwner(ScreenType.valueOf(screen), documentId, projectId),
        ScreenStateJson.decode(states),
    )
    companion object : DatabaseModelFactory<ScreenState, ScreenStateEntity> {
        override fun fromDomainModel(domainModel: ScreenState) = ScreenStateEntity(
            key = "${domainModel.owner.screen.name}:${domainModel.owner.documentId ?: "global"}",
            screen = domainModel.owner.screen.name,
            documentId = domainModel.owner.documentId,
            states = domainModel.states.toString(),
            projectId = domainModel.owner.projectId,
        )
    }
}
