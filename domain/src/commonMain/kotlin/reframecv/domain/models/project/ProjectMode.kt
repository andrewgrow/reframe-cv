package reframecv.domain.models.project

/** Keeps subproject organization and document workspaces mutually exclusive. */
enum class ProjectMode {
    Unconfigured,
    Container,
    Workspace,
}
