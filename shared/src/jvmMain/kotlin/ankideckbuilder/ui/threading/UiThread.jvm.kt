package ankideckbuilder.ui.threading

import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing

/** Executes [block] on the Swing UI thread, blocking the caller until it completes. */
fun <T> runOnUiThread(block: () -> T): T = if (SwingUtilities.isEventDispatchThread()) {
    block()
} else {
    runBlocking(Dispatchers.Swing) { block() }
}
