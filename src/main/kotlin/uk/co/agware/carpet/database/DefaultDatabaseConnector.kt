package uk.co.agware.carpet.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.exception.MagicCarpetException
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.SQLException
import javax.naming.InitialContext
import javax.sql.DataSource
/**
 * // TODO Formatting, detail etc
 * Default Database Connector implementation
 *
 * Created by Simon on 29/12/2016.
 */
// TODO I'd simplify this class to only accepting a "connection" object to be honest, needing to support all these
// TODO different ways to set the connection is pointless, the people using the lib can just do it themselves and
// TODO supply what is easiest for us

// TODO All these methods that return boolean, its pointless to do so, they either return true or throw an exception

// TODO This class misses any of the actual changes I asked for in it:
// TODO     * It should create a "hash" column on the ChangeSet table in which to store a hash of the query being executed
// TODO     * It should, when creating the table, check for this column separately to the standard "does the table exist"
// TODO       as this is a new column and we need to support backwards compatibility with existing users

// TODO The exception handling needs to be better, this class is literally doing the exact thing I said it shouldn't
// TODO which is simply wrapping the checked exceptions into an unchecked one without any extra detail where possible
open class DefaultDatabaseConnector : DatabaseConnector {

    companion object {
        private val TABLE_NAME = "change_set"
        private val VERSION_COLUMN = "version"
        private val TASK_COLUMN = "task"
        private val DATE_COLUMN = "applied"
    } // TODO Space here
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private var connection: Connection? = null

    @Override
    override fun setConnection(connection: Connection) {
        try {
            this.connection = connection
            this.connection!!.autoCommit = false
        } catch (e: SQLException) {
            throw MagicCarpetException (e.message, e)
        }
    }

    @Override
    override fun setConnection(jdbcName: String) {
        try {
            val source = InitialContext().lookup("java:comp/env/jdbc/" + jdbcName) as DataSource
            this.connection = source.connection
            this.connection!!.autoCommit = false
        } catch (e: Exception) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun setConnection(connectionUrl: String, name: String, password: String) {
        try {
            this.connection = DriverManager.getConnection(connectionUrl, name, password)
            this.connection!!.autoCommit = false
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun commit(): Boolean{
        try {
            this.connection!!.commit()
            return true
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)

        }
    }

    @Override
    override fun close(): Boolean{
        try {
            this.connection!!.close()
            return true
        } catch (e: SQLException) {
           throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun executeStatement(sql: String): Boolean{
        try {
            val statement = this.connection!!.createStatement()
            this.logger.info("Executing statement: {}", sql)
            statement.execute(sql)
            return true
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun insertChange(version: String, taskName: String): Boolean{
        val sql: String = "INSERT INTO $TABLE_NAME ($VERSION_COLUMN,$TASK_COLUMN,$DATE_COLUMN) VALUES (?,?,?)"
        try {
            val preparedStatement = this.connection!!.prepareStatement(sql)
            preparedStatement.setString(1, version)
            preparedStatement.setString(2, taskName)
            preparedStatement.setDate(3, Date(System.currentTimeMillis()))
            preparedStatement.execute()
            return true
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun checkChangeSetTable(createTable: Boolean) {
        try {
            val dbm = this.connection!!.metaData
            val tables = dbm.getTables(null, null, TABLE_NAME, null)
            if(!tables.next() && createTable){
                val statement = this.connection!!.createStatement()
                val createTableStatement: String = "CREATE TABLE $TABLE_NAME ($VERSION_COLUMN VARCHAR(255), $TASK_COLUMN VARCHAR(255), $DATE_COLUMN DATE)"
                statement.executeUpdate(createTableStatement)
                commit()
            }
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun changeExists(version: String, taskName: String): Boolean{
        val query: String = "SELECT * FROM $TABLE_NAME WHERE $VERSION_COLUMN = ? AND $TASK_COLUMN = ?"
        try {
            val statement = this.connection!!.prepareStatement(query)
            statement.setString(1, version)
            statement.setString(2, taskName)
            return statement.executeQuery().next()
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun rollBack() {
        try {
            this.connection!!.rollback()
        } catch (e: SQLException) {
            throw MagicCarpetException(e.message, e)
        }
    }

}
