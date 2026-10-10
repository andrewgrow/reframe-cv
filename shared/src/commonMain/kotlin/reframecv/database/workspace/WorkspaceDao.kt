package reframecv.database.workspace

import androidx.room3.Query

/** Shared queries used inside the concrete DAOs' transactions. */
interface WorkspaceDao {
    @Query(
        "UPDATE projects SET mode = 'Workspace', updated_at = :updatedAt " +
            "WHERE id = :id AND deleted_at IS NULL AND mode IN ('Unconfigured', 'Workspace') " +
            "AND NOT EXISTS (SELECT 1 FROM projects AS child WHERE " +
            "child.parent_id = :id AND child.deleted_at IS NULL)",
    )
    suspend fun markWorkspace(id: Long, updatedAt: Long): Int

    suspend fun requireWorkspace(id: Long, updatedAt: Long) {
        check(markWorkspace(id, updatedAt) == 1) {
            "Project is missing, deleted, or contains subprojects"
        }
    }

    @Query(
        "SELECT COUNT(*) FROM resumes WHERE id = :id AND project_id = " +
            ":projectId AND deleted_at IS NULL",
    )
    suspend fun activeResumeCount(id: Long, projectId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM cover_letters WHERE id = :id AND project_id = " +
            ":projectId AND deleted_at IS NULL",
    )
    suspend fun activeLetterCount(id: Long, projectId: Long): Int

    suspend fun requireActiveResume(id: Long, projectId: Long) {
        require(activeResumeCount(id, projectId) == 1) {
            "Resume is missing, deleted, or belongs to another project"
        }
    }

    suspend fun requireActiveLetter(id: Long, projectId: Long) {
        require(activeLetterCount(id, projectId) == 1) {
            "Letter is missing, deleted, or belongs to another project"
        }
    }

    @Query(
        "UPDATE projects SET mode = 'Unconfigured', updated_at = :updatedAt " +
            "WHERE id = :id AND mode = 'Workspace' AND deleted_at IS NULL " +
            "AND NOT EXISTS (SELECT 1 FROM resumes WHERE project_id = :id AND " +
            "deleted_at IS NULL) " +
            "AND NOT EXISTS (SELECT 1 FROM vacancies WHERE project_id = :id) " +
            "AND NOT EXISTS (SELECT 1 FROM cover_letters WHERE project_id = " +
            ":id AND deleted_at IS NULL)",
    )
    suspend fun resetEmptyWorkspace(id: Long, updatedAt: Long)

    @Query(
        "UPDATE projects SET main_resume_id = NULL, updated_at = :updatedAt " +
            "WHERE main_resume_id = :id",
    )
    suspend fun clearMainResume(id: Long, updatedAt: Long)

    @Query(
        "UPDATE resumes SET source_resume_id = NULL, updated_at = :updatedAt " +
            "WHERE source_resume_id = :id",
    )
    suspend fun clearSourceResume(id: Long, updatedAt: Long)

    @Query("UPDATE vacancies SET resume_id = NULL, updated_at = :updatedAt WHERE resume_id = :id")
    suspend fun clearVacancyResumes(id: Long, updatedAt: Long)

    @Query(
        "UPDATE vacancies SET cover_letter_id = NULL, updated_at = :updatedAt " +
            "WHERE cover_letter_id = :id",
    )
    suspend fun clearVacancyLetters(id: Long, updatedAt: Long)
}
