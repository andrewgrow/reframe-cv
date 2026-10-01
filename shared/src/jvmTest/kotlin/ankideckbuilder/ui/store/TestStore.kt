package ankideckbuilder.ui.store

import com.arkivanov.mvikotlin.core.rx.Disposable
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.core.store.Store

internal class TestStore : Store<String, String, Nothing> {
    override var state = "Initial"
        private set
    override var isDisposed = false
        private set
    private val observers = mutableListOf<Observer<String>>()
    val subscriberCount: Int get() = observers.size
    var subscribersAtDisposal: Int? = null
        private set

    override fun states(observer: Observer<String>): Disposable {
        observers += observer
        observer.onNext(state)
        return Disposable { observers -= observer }
    }

    override fun labels(observer: Observer<Nothing>): Disposable = Disposable()

    override fun accept(intent: String) {
        check(!isDisposed)
        state = intent
        observers.toList().forEach { it.onNext(state) }
    }

    override fun init() = Unit

    override fun dispose() {
        subscribersAtDisposal = subscriberCount
        isDisposed = true
        observers.clear()
    }
}
