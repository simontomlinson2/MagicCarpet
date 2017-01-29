package uk.co.agware.carpet

import com.nhaarman.mockito_kotlin.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import uk.co.agware.carpet.database.DefaultDatabaseConnector
import uk.co.agware.carpet.stubs.ResultsSetStub
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.test.assertEquals

//TODO Rollback, CheckHashMatches, UpdateHash

@RunWith(JUnitPlatform::class)
class TestDatabaseConnector: Spek({



    describe("A Database Connection object") {

        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        on("executing changes") {
            subject.executeStatement("SELECT * FROM Table")

            it("Should execute the statement on the database") {
                verify(statement).execute("SELECT * FROM Table")
            }
        }

        on("commiting changes") {
            subject.commit()
            it("Should commit the change to the database") {
                verify(connection).commit()
            }
        }

        on("Closing the database connection") {
            subject.close()
            it("Should close the connection to the database") {
                verify(connection).close()
            }
        }

       on("Recording the task") {
            subject.recordTask("1.0.0", "Create DB", "SELECT * FROM Table")
            val expectedStatement = """INSERT INTO change_set
                             (version, task, applied, hash)
                             VALUES (?, ?, ?, ?)"""
            val statementCaptor = argumentCaptor<String>()

            it("Should prepare the statement") {
                verify(connection).prepareStatement(statementCaptor.capture())

            }

            it("Should execute the statement") {
                verify(preparedStatement).execute()
            }

            it("should commit the changes to the database") {
                verify(connection).commit()
            }


            it("Should record populate the statement with the task values") {
                assertEquals(expectedStatement, statementCaptor.value)
                verify(preparedStatement).setString(1, "1.0.0")
                verify(preparedStatement).setString(2, "Create DB")
                verify(preparedStatement).setInt(3, "SELECT * FROM Table".hashCode())
                verify(preparedStatement).setDate(any(), any())
            }

        }
    }

    describe("A DatabaseConnector Object"){
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        whenever(preparedStatement.executeQuery()).thenReturn(ResultsSetStub(true))
        on("Checking the version exists") {
            val expectedStatement = """"SELECT * FROM change_set
                      WHERE version = ?"""
            val statementCaptor = argumentCaptor<String>()
            val version = "1.0.0"
            subject.versionExists(version)
            it("Should prepare the statement") {
                verify(connection).prepareStatement(statementCaptor.capture())
                assertEquals(expectedStatement, statementCaptor.value)
            }

            it("Should record populate the statement with the version values") {

                verify(preparedStatement).setString(1, version)
            }

            it("Should execute the statement") {
                verify(preparedStatement).executeQuery()
            }
        }
    }

    describe("A DatabaseConnector Object"){
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        whenever(preparedStatement.executeQuery()).thenReturn(ResultsSetStub(true))
        on("Checking the task exists") {
            val expectedStatement =  """SELECT * FROM change_set
                        WHERE version = ?
                            AND task = ?
                     """
            val statementCaptor = argumentCaptor<String>()
            val version = "1.0.0"
            val taskName = "Task Name"
            subject.taskExists(version, taskName)
            it("Should prepare the statement") {
                verify(connection).prepareStatement(statementCaptor.capture())
                assertEquals(expectedStatement, statementCaptor.value)
            }

            it("Should record populate the statement with the version values") {
                verify(preparedStatement).setString(1, version)
            }

            it("Should record populate the statement with the task name values") {
                verify(preparedStatement).setString(2, taskName)
            }

            it("Should execute the statement") {
                verify(preparedStatement).executeQuery()
            }
        }
    }

    describe("A DefaultDatabaseConnection with no change set table") {
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val metaData = mock<DatabaseMetaData>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        whenever(connection.metaData).thenReturn(metaData)
        whenever(metaData.getTables(null, null, "change_set", null)).thenReturn(ResultsSetStub(false))
        whenever(metaData.getColumns(null, null, "change_set", "hash")).thenReturn(ResultsSetStub(true))

        on("checking the existence of the change set table") {

            subject.checkChangeSetTable(true)

            it("should check the metadata for the tables existence") {
                verify(metaData).getTables(null, null, "change_set", null)
            }

            it("should create the change set table") {
                val expectedStatement = """CREATE TABLE change_set (
                                                         version VARCHAR(255),
                                                         task VARCHAR(255),
                                                         applied DATE
                                                      )"""
                verify(statement).execute(check {
                    assertEquals(it, expectedStatement)
                })
            }

            it("should commit the changes to the database") {
                verify(connection).commit()
            }

        }
    }

    describe("A DefaultDatabaseConnection with a change set table") {
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        val metaData = mock<DatabaseMetaData>()
        whenever(connection.metaData).thenReturn(metaData)
        whenever(metaData.getTables(null, null, "change_set", null)).thenReturn(ResultsSetStub(true))
        whenever(metaData.getColumns(null, null, "change_set", "hash")).thenReturn(ResultsSetStub(true))
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)

        on("checking the existence of the change set table that exists") {

              subject.checkChangeSetTable(true)

            it("should check the metadata for the tables existence") {
                verify(metaData).getTables(null, null, "change_set", null)
            }

            it("Should not update the table schema for the hash column") {

                verify(statement, never()).execute(any())
            }

            it("should not commit the changes to the database") {

                verify(connection, never()).commit()
            }
        }
    }

    describe("A DefaultDatabaseConnection with no hash column") {
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val metaData = mock<DatabaseMetaData>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        val subject = DefaultDatabaseConnector(connection)
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        whenever(connection.metaData).thenReturn(metaData)
        whenever(metaData.getTables(null, null, "change_set", null)).thenReturn(ResultsSetStub(true))
        whenever(metaData.getColumns(null, null, "change_set", "hash")).thenReturn(ResultsSetStub(false))

        on("checking for the hash column that does not exist") {
             subject.checkChangeSetTable(true)

            it("Should check for the hash columns existence in the table") {
                verify(metaData).getColumns(null, null, "change_set", "hash")
            }

            it("Should update the table schema for the hash column") {
                val expectedStatement = "ALTER TABLE change_set ADD COLUMN hash VARCHAR(64)"
                verify(statement).execute(check {
                    assertEquals(it, expectedStatement)
                })
            }

            it("should commit the changes to the database") {
                verify(connection).commit()
            }
        }
    }

    describe("A DefaultDatabaseConnection with a hash column") {
        val statement = mock<Statement>()
        val preparedStatement = mock<PreparedStatement>()
        val connection = mock<Connection> {
            on { createStatement() } doReturn statement
            on { prepareStatement(any()) } doReturn preparedStatement
        }
        whenever(preparedStatement.execute()).thenReturn(true)
        whenever(statement.execute(any())).thenReturn(true)
        val subject = DefaultDatabaseConnector(connection)
        val metaData = mock<DatabaseMetaData>()
        whenever(connection.metaData).thenReturn(metaData)
        whenever(metaData.getTables(null, null, "change_set", null)).thenReturn(ResultsSetStub(true))
        whenever(metaData.getColumns(null, null, "change_set", "hash")).thenReturn(ResultsSetStub(true))

        on("checking for the hash column that exists") {
             subject.checkChangeSetTable(true)
            it("Should check for the hash columns existence in the table") {
                verify(metaData).getColumns(null, null, "change_set", "hash")
            }

            it("Should not update the table schema for the hash column") {

                verify(statement, never()).execute(any())
            }

            it("should not commit the changes to the database") {
                verify(connection, never()).commit()
            }


        }
    }


 })
