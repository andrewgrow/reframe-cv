package reframecv.database.providers

/** Groups database access for provider connections, reusable runners and their history. */
interface ProviderDatabase {
    fun providerConnectionsDao(): ProviderConnectionsDao
    fun connectionRunnersDao(): ConnectionRunnersDao
    fun runnerResultImportsDao(): RunnerResultImportsDao
    fun runnerResultsDao(): RunnerResultsDao
}
