package uk.co.agware.carpet.database

/**
 * Set up a database connection
 * Controls interaction with the database.
 *
 * Created by Simon on 29/12/2016.
 */
interface DatabaseConnector: AutoCloseable {

    /**
     * Commit Changes to the database
     * @return Boolean
     */
    fun commit()

    /**
     * close the connection to the database
     * @return Boolean
     */
    override fun close()

    /**
     * Insert a change into the database changes table
     * Inserts version, task name and hash of query run
     * @param version The change version
     * @param taskName the name of the task executed
     * @param query the query to insert
     * @return Boolean
     */
    fun insertChange(version: String, taskName: String, query: String)

    /**
     * execute a statement on the database
     * @param sql SQL statement to execute
     * @return Boolean
     */
    fun executeStatement(sql: String)

    /**
     * Create the changes table in the database if it does not exist and *createTable* is true
     * @param createTable Create the changes table if it does not exist
     * @return Boolean
     */
    fun checkChangeSetTable(createTable: Boolean)

    /**
     * Check if a change task exists in the database using the version, task name and hash of query
     * @param version The Change Version
     * @param taskName The Name of the task
     * @param query The query to check.
     * @return Boolean
     */
    fun changeExists(version: String, taskName: String, query: String): Boolean

    /**
     * Update a tasks table with new query hash if it doesn't already have one
     * @param version The Change Version
     * @param taskName The Name of the task
     * @param query The query to update.
     * @return Boolean
     */
    fun updateChange(version: String, taskName: String, query: String)

    /**
     * Rollback changes on the database
     */
    fun rollBack()
}
