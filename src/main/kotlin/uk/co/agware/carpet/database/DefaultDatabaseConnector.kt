package uk.co.agware.carpet.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.exception.MagicCarpetException
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException

/**
 * Default Database Connector implementation.
 * Creates a connection to a database.
 * Sets up the table to store changes that have been applied to the database
 *
 * Created by Simon on 29/12/2016.
 */
open class DefaultDatabaseConnector : DatabaseConnector {

    companion object {
        private val TABLE_NAME = "change_set"
        private val VERSION_COLUMN = "version"
        private val TASK_COLUMN = "task"
        private val HASH_COLUMN = "query_hash"
        private val DATE_COLUMN = "applied"
    }

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private var connection: Connection? = null

    @Override
    override fun setConnection(connection: Connection) {
        try {
            this.connection = connection
            this.connection!!.autoCommit = false
        } catch (e: SQLException) {
            throw MagicCarpetException ("Could not connect to database", e)
        }
    }

    @Override
    override fun commit(){
        try {
            this.connection!!.commit()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not commit changes to database", e)

        }
    }

    @Override
    override fun close(){
        try {
            this.connection!!.close()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not close database connection", e)
        }
    }

    @Override
    override fun executeStatement(sql: String){
        try {
            val statement = this.connection!!.createStatement()
            this.logger.info("Executing statement: {}", sql)
            statement.execute(sql)
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not execute statement: $sql", e)
        }
    }

    @Override
    override fun insertChange(version: String, taskName: String, query: String){
        val sql: String = "INSERT INTO $TABLE_NAME ($VERSION_COLUMN,$TASK_COLUMN,$HASH_COLUMN,$DATE_COLUMN) VALUES (?,?,?,?)"
        try {
            val preparedStatement = this.connection!!.prepareStatement(sql)
            preparedStatement.setString(1, version)
            preparedStatement.setString(2, taskName)
            preparedStatement.setInt(3, query.hashCode())
            preparedStatement.setDate(4, Date(System.currentTimeMillis()))
            preparedStatement.execute()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not insert task: $taskName for change: $version", e)
        }
    }

    @Override
    override fun checkChangeSetTable(createTable: Boolean) {
        try {
            val dbm = this.connection!!.metaData
            val tables = dbm.getTables(null, null, TABLE_NAME, null)
            if(!tables.next() && createTable){
                val statement = this.connection!!.createStatement()
                val createTableStatement: String = "CREATE TABLE $TABLE_NAME ($VERSION_COLUMN VARCHAR(255), $TASK_COLUMN VARCHAR(255), $HASH_COLUMN BIGINT, $DATE_COLUMN DATE)"
                statement.executeUpdate(createTableStatement)
                commit()
            }
            else if (tables.next()){
                checkUpdateTable(createTable)
            }
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not create table", e)
        }
    }

    private fun checkUpdateTable(updateTable: Boolean){
        try {
            val dbm = this.connection!!.metaData
            val columns = dbm.getColumns(null, null, TABLE_NAME, HASH_COLUMN)
            if(!columns.next() && updateTable){
                val statement = this.connection!!.createStatement()
                val createTableStatement: String = "ALTER TABLE $TABLE_NAME ADD COLUMN $HASH_COLUMN BIGINT"
                statement.executeUpdate(createTableStatement)
                commit()
            }
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not alter table: $TABLE_NAME with Column: $HASH_COLUMN", e)
        }
    }

    @Override
    override fun changeExists(version: String, taskName: String, query: String): Boolean{
        //To support older version if query is null don't check for it
        val select: String = "SELECT * FROM $TABLE_NAME WHERE $VERSION_COLUMN = ? AND $TASK_COLUMN = ?" +
                " AND ($HASH_COLUMN = ? OR $HASH_COLUMN IS NULL)"
        try {
            val statement = this.connection!!.prepareStatement(select)
            statement.setString(1, version)
            statement.setString(2, taskName)
            statement.setInt(3, query.hashCode())
            return statement.executeQuery().next()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not execute statement: $select", e)
        }
    }

    @Override
    override fun updateChange(version: String, taskName: String, query: String){
        //Update the row using version and task name where query is null
        val select: String = "UPDATE $TABLE_NAME SET $HASH_COLUMN = ? WHERE $VERSION_COLUMN = ? AND $TASK_COLUMN = ?" +
                " AND $HASH_COLUMN IS NULL"
        try {
            val statement = this.connection!!.prepareStatement(select)
            statement.setInt(1, query.hashCode())
            statement.setString(2, version)
            statement.setString(3, taskName)
            statement.executeQuery()
            commit()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not execute statement: $select", e)
        }
    }

    @Override
    override fun rollBack() {
        try {
            this.connection!!.rollback()
        } catch (e: SQLException) {
            throw MagicCarpetException("Could not roll back changes", e)
        }
    }

}
