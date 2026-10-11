package reframecv.logging

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.logger.Logger
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory

fun createHistoryStoreFactory(history: EventHistory): StoreFactory = LoggingStoreFactory(
    delegate = DefaultStoreFactory(),
    logger = object : Logger {
        override fun log(text: String) = history.record(text)
    },
    logFormatter = SummaryLogFormatter(),
)
