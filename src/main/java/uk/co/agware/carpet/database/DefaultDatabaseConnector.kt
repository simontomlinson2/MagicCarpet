package uk.co.agware.carpet.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.exception.MagicCarpetException
import java.sql.*
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.sql.DataSource
/**
 * Created by Simon on 29/12/2016.
 */
open class DefaultDatabaseConnector : DatabaseConnector {

    private val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)
    companion object {
        private val TABLE_NAME = "change_set"
        private val VERSION_COLUMN = "version"
        private val TASK_COLUMN = "task"
        private val DATE_COLUMN = "applied"
    }
    private var connection: Connection? = null

    @Override
    override fun setConnection(con: Connection) {
        try {
            this.connection = con
            this.connection!!.autoCommit = false
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            throw MagicCarpetException (e.message, e)
        }
    }

    @Override
    override fun setConnection(jdbcName: String) {
        try {
            var source: DataSource = InitialContext().lookup("java:comp/env/jdbc/" + jdbcName) as DataSource
            this.connection = source.connection
            this.connection!!.autoCommit = false
        } catch (e: Exception) {
            when(e){
                is NamingException , is SQLException -> {
                    this.LOGGER.error(e.message, e)
                }
                else -> throw e
            }
            this.LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun setConnection(connectionUrl: String, name: String, password: String) {
        try {
            this.connection = DriverManager.getConnection(connectionUrl, name, password)
            this.connection!!.autoCommit = false
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun commit(): Boolean{
        try {
            this.connection!!.commit()
            return true
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            return false
        }
    }

    @Override
    override fun close(): Boolean{
        try {
            this.connection!!.close()
            return true
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            return false
        }
    }

    @Override
    override fun executeStatement(sql: String): Boolean{
        try {
            val statement: Statement = this.connection!!.createStatement()
            this.LOGGER.info("Executing statement: {}", sql)
            statement.execute(sql)
            return true
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            return false
        }
    }

    @Override
    override fun insertChange(version: String, taskName: String): Boolean{
        val sql: String = String.format("INSERT INTO %s (%s,%s,%s) VALUES (?,?,?)", TABLE_NAME, VERSION_COLUMN, TASK_COLUMN, DATE_COLUMN)
        try {
            val preparedStatement: PreparedStatement = this.connection!!.prepareStatement(sql)
            preparedStatement.setString(1, version)
            preparedStatement.setString(2, taskName)
            preparedStatement.setDate(3, Date(System.currentTimeMillis()))
            preparedStatement.execute()
            return true
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            return false
        }
    }

    @Override
    override fun checkChangeSetTable(createTable: Boolean) {
        try {
            var dbm: DatabaseMetaData = this.connection!!.metaData
            var tables: ResultSet = dbm.getTables(null, null, TABLE_NAME, null)
            if(!tables.next() && createTable){
                var statement: Statement = this.connection!!.createStatement()
                var createTableStatement: String = String.format("CREATE TABLE %s (%s VARCHAR(255), %s VARCHAR(255), %s DATE)", TABLE_NAME, VERSION_COLUMN, TASK_COLUMN, DATE_COLUMN)
                statement.executeUpdate(createTableStatement)
                commit()
            }
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)
        }
    }

    @Override
    override fun changeExists(changeVersion: String, taskName: String): Boolean{
        var query: String = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", TABLE_NAME, VERSION_COLUMN, TASK_COLUMN)
        try {
            var statement = this.connection!!.prepareStatement(query)
            statement.setString(1, changeVersion)
            statement.setString(2, taskName)
            return statement.executeQuery().next()
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
            return false
        }
    }

    @Override
    override fun rollBack() {
        try {
            this.connection!!.rollback()
        } catch (e: SQLException) {
            this.LOGGER.error(e.message, e)
        }
    }

}
