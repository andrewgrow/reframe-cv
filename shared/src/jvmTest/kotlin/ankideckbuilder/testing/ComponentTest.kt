package ankideckbuilder.testing

import ankideckbuilder.ui.threading.runOnUiThread
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import org.junit.After
import org.junit.Before

abstract class ComponentTest {
    protected lateinit var lifecycle: LifecycleRegistry
        private set

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
