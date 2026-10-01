package ankideckbuilder.ui.threading

import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UiThreadTest {
    @Test
    fun dispatchesToUiThreadAndReturnsResult() {
        assertFalse(SwingUtilities.isEventDispatchThread())
        val expected = Any()

        val result = runOnUiThread {
            assertTrue(SwingUtilities.isEventDispatchThread())
            expected
        }

        assertSame(expected, result)
    }

    @Test
    fun executesDirectlyWhenAlreadyOnUiThread() {
        SwingUtilities.invokeAndWait {
            val expected = Any()
            val thread = Thread.currentThread()
            val result = runOnUiThread {
                assertSame(thread, Thread.currentThread())
                expected
            }
            assertSame(expected, result)
        }
    }

    @Test
    fun propagatesExceptionsToCaller() {
        val expected = IllegalStateException("Test failure")

        val actual = assertFailsWith<IllegalStateException> {
            runOnUiThread<Unit> { throw expected }
        }

        assertSame(expected, actual)
    }
}
