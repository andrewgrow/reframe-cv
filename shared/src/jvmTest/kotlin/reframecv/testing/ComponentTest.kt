package reframecv.testing

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import org.junit.After
import org.junit.Before
import reframecv.ui.threading.runOnUiThread

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
