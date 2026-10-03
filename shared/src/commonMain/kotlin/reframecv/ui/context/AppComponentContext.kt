package reframecv.ui.context

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.GenericComponentContext
import reframecv.dependencies.ApplicationDependencies

interface AppComponentContext : GenericComponentContext<AppComponentContext> {
    val dependencies: ApplicationDependencies
}

class DefaultAppComponentContext(
    private val componentContext: ComponentContext,
    override val dependencies: ApplicationDependencies,
) : AppComponentContext {
    override val lifecycle get() = componentContext.lifecycle
    override val stateKeeper get() = componentContext.stateKeeper
    override val instanceKeeper get() = componentContext.instanceKeeper
    override val backHandler get() = componentContext.backHandler

    override val componentContextFactory = ComponentContextFactory<AppComponentContext> {
            lifecycle,
            stateKeeper,
            instanceKeeper,
            backHandler,
        ->
        DefaultAppComponentContext(
            DefaultComponentContext(lifecycle, stateKeeper, instanceKeeper, backHandler),
            dependencies,
        )
    }
}
