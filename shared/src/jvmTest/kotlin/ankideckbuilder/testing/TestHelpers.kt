package ankideckbuilder.testing

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun getTestString(resource: StringResource): String = runBlocking {
    getString(resource)
}
