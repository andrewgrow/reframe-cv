package reframecv.ui.components.projects.dashboard

import reframecv.ui.context.AppComponentContext

interface DashboardComponent {
    val projectId: Long
}

class DefaultDashboardComponent(
    componentContext: AppComponentContext,
    override val projectId: Long,
) : DashboardComponent,
    AppComponentContext by componentContext
