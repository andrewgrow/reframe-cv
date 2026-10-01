package ankideckbuilder.ui.store

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store

/**
 * Exposes an initialized [store] as a [Value] for the lifetime of [lifecycle].
 * Call on the UI thread with a live lifecycle and an exclusively owned store.
 * The subscription and store are disposed when the lifecycle is destroyed.
 * Do not use this binding for shared or retained stores with a different lifetime.
 */
fun <State : Any> bindStoreToLifecycle(
    store: Store<*, State, *>,
    lifecycle: Lifecycle,
): Value<State> {
    require(lifecycle.state != Lifecycle.State.DESTROYED) { "Lifecycle is already destroyed" }
    require(!store.isDisposed) { "Store is already disposed" }

    val value = MutableValue(store.state)
    val subscription = store.states(observer { value.value = it })
    lifecycle.doOnDestroy {
        subscription.dispose()
        store.dispose()
    }
    return value
}
