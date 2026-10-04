package reframecv.domain.models.project

/** A mode is committed only when the project first receives children or workspace data. */
enum class ProjectMode {
    Unconfigured,
    Container,
    Workspace,
}
