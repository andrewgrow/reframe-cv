package reframecv.database.coverletter

import androidx.room3.Dao
import androidx.room3.Transaction
import reframecv.database.workspace.WorkspaceDao
import reframecv.database.workspace.normalizeKeywords
import reframecv.domain.models.coverletter.CoverLetter

/** Saves, loads, and searches cover letters in the local database. */
@Dao
interface CoverLetterDao :
    CoverLetterQueries,
    WorkspaceDao {
    @Transaction
    suspend fun create(record: CoverLetter): Long {
        requireWorkspace(record.projectId, record.createdAt)
        val id = insertEntity(CoverLetterEntity.fromDomainModel(record))
        replaceKeywords(id, record.keywords)
        return id
    }

    @Transaction
    suspend fun update(record: CoverLetter) {
        val existing = checkNotNull(findById(record.id)) { "Record does not exist" }
        require(existing.entity.projectId == record.projectId) {
            "Cannot move a record to another project"
        }
        requireWorkspace(record.projectId, record.updatedAt)
        check(updateEntity(CoverLetterEntity.fromDomainModel(record)) == 1)
        replaceKeywords(record.id, record.keywords)
    }

    @Transaction
    suspend fun replaceKeywords(id: Long, keywords: List<String>) {
        clearKeywords(id)
        insertKeywords(
            normalizeKeywords(keywords).mapIndexed { position, keyword ->
                CoverLetterKeywordEntity(id, keyword, keyword.lowercase(), position)
            },
        )
    }

    @Transaction
    suspend fun search(projectId: Long, keyword: String): List<CoverLetterWithKeywords> =
        searchNormalized(projectId, keyword.trim().lowercase())

    @Transaction
    suspend fun delete(id: Long, updatedAt: Long): Int {
        val projectId = findById(id)?.entity?.projectId ?: return 0
        clearVacancyLetters(id, updatedAt)
        val changed = deleteEntity(id)
        if (changed > 0) resetEmptyWorkspace(projectId, updatedAt)
        return changed
    }
}
