package ankideckbuilder.ui.store

import ankideckbuilder.testing.ComponentTest
import ankideckbuilder.ui.threading.runOnUiThread
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before

class StoreBindingTest : ComponentTest() {
    @Before
    fun createLifecycle() = runOnUiThread {
        lifecycle.create()
    }

    @Test
    fun exposesCurrentStateAndSubsequentUpdates() = runOnUiThread {
        val store = TestStore()
        val value = bindStoreToLifecycle(store, lifecycle)

        assertEquals("Initial", value.value)
        assertEquals(1, store.subscriberCount)
        store.accept("Updated")
        assertEquals("Updated", value.value)
        assertFalse(store.isDisposed)
    }

    @Test
    fun unsubscribesBeforeDisposingStoreOnDestroy() = runOnUiThread {
        val store = TestStore()
        val value = bindStoreToLifecycle(store, lifecycle)
        store.accept("Last state")

        lifecycle.destroy()

        assertEquals(0, store.subscriberCount)
        assertEquals(0, store.subscribersAtDisposal)
        assertTrue(store.isDisposed)
        assertEquals("Last state", value.value)
    }

    @Test
    fun rejectsDestroyedLifecycleWithoutTakingOwnership() = runOnUiThread {
        lifecycle.destroy()
        val store = TestStore()

        assertFailsWith<IllegalArgumentException> { bindStoreToLifecycle(store, lifecycle) }

        assertEquals(0, store.subscriberCount)
        assertFalse(store.isDisposed)
        store.dispose()
    }

    @Test
    fun rejectsDisposedStoreWithoutSubscribing() = runOnUiThread {
        val store = TestStore()
        store.dispose()

        assertFailsWith<IllegalArgumentException> { bindStoreToLifecycle(store, lifecycle) }

        assertEquals(0, store.subscriberCount)
    }
}
