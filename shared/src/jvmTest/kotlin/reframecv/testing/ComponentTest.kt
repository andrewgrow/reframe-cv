package reframecv.testing

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import org.junit.After
import org.junit.Before
import reframecv.dependencies.TestApplicationDependencies
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

abstract class ComponentTest {
    protected lateinit var lifecycle: LifecycleRegistry
        private set

    protected val dependencies = TestApplicationDependencies()

    protected fun appComponentContext() =
        DefaultAppComponentContext(DefaultComponentContext(lifecycle), dependencies)

    @Before
    fun setUpLifecycle() = runOnUiThread {
        lifecycle = LifecycleRegistry()
    }

    @After
    fun tearDownLifecycle() = runOnUiThread {
        if (::lifecycle.isInitialized) {
            lifecycle.destroy()
        }
    }
}
