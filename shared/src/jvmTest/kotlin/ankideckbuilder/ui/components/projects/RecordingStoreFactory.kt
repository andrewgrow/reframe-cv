package ankideckbuilder.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory

internal class RecordingStoreFactory : StoreFactory {
    lateinit var store: Store<*, *, *>

    override fun <Intent : Any, Action : Any, Message : Any, State : Any, Label : Any> create(
        name: String?,
        autoInit: Boolean,
        initialState: State,
        bootstrapper: Bootstrapper<Action>?,
        executorFactory: () -> Executor<Intent, Action, State, Message, Label>,
        reducer: Reducer<State, Message>,
    ): Store<Intent, State, Label> = DefaultStoreFactory().create(
        name = name,
        autoInit = autoInit,
        initialState = initialState,
        bootstrapper = bootstrapper,
        executorFactory = executorFactory,
        reducer = reducer,
    ).also { store = it }
}
