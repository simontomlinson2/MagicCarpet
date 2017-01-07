package uk.co.agware.carpet.database

import java.sql.Connection
/**
 * Created by Simon on 29/12/2016.
 */
interface DatabaseConnector {

    fun setConnection(jdbcName: String)

    fun setConnection(connectionUrl: String, name: String, password: String)

    fun setConnection(connection: Connection)

    fun commit(): Boolean

    fun close(): Boolean

    fun insertChange(version: String, taskName: String): Boolean

    fun executeStatement(sql: String): Boolean

    fun checkChangeSetTable(createTable: Boolean)

    fun changeExists(version: String, taskName: String): Boolean

    fun rollBack()
}