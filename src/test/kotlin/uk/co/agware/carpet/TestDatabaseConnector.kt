package uk.co.agware.carpet

import com.nhaarman.mockito_kotlin.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import uk.co.agware.carpet.database.DefaultDatabaseConnector
import uk.co.agware.carpet.database.toMD5
import uk.co.agware.carpet.stubs.ResultsSetStub
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

//TODO CheckHashMatches, UpdateHash

@RunWith(JUnitPlatform::class)
class TestDatabaseConnector: Spek(
        {
            describe("A Database Connection") {

                var statement: Statement? = null
                var preparedStatement: PreparedStatement? = null
                var connection: Connection? = null
                var metaData: DatabaseMetaData? = null

                beforeEachTest {
                    statement = mock<Statement>()
                    preparedStatement = mock<PreparedStatement>()
                    connection = mock<Connection>()
                    metaData = mock<DatabaseMetaData>()
                    whenever(connection!!.createStatement()).thenReturn(statement)
                    whenever(connection!!.prepareStatement(any())).thenReturn(preparedStatement)
                    whenever(connection!!.metaData).thenReturn(metaData)
                    whenever(preparedStatement!!.execute()).thenReturn(true)
                    whenever(statement!!.execute(any())).thenReturn(true)
                    whenever(preparedStatement!!.executeQuery()).thenReturn(ResultsSetStub(true))

                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)
                    }

                    it("Should execute the statement on the database") {
                        val select = "SELECT * FROM Table"
                        subject!!.executeStatement(select)
                        verify(statement)!!.execute(select)
                    }

                    it("Should commit the change to the database") {
                        subject!!.commit()
                        verify(connection)!!.commit()
                    }

                    it("Should rollback the database changes") {
                        subject!!.rollBack()
                        verify(connection)!!.rollback()
                    }

                    it("Should close the connection to the database") {
                        subject!!.close()
                        verify(connection)!!.close()
                    }

                    on("recording the task") {

                        val expectedStatement = """INSERT INTO change_set
                             (version, task, applied, hash)
                             VALUES (?, ?, ?, ?)"""
                        val statementCaptor = argumentCaptor<String>()

                        subject!!.recordTask("1.0.0", "Create DB", "SELECT * FROM Table")

                        it("Should prepare the statement") {
                            verify(connection)!!.prepareStatement(statementCaptor.capture())
                        }

                        it("Should set the statement values") {
                            assertEquals(expectedStatement, statementCaptor.value)
                            verify(preparedStatement)!!.setString(1, "1.0.0")
                            verify(preparedStatement)!!.setString(2, "Create DB")
                            verify(preparedStatement)!!.setInt(3, "SELECT * FROM Table".hashCode())
                            verify(preparedStatement)!!.setDate(any(), any())
                        }

                        it("Should execute the statement") {
                            verify(preparedStatement)!!.execute()
                        }

                    }
                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)
                    }

                    on("checking a version exists") {
                        val expectedStatement = """"SELECT * FROM change_set
                      WHERE version = ?"""
                        val statementCaptor = argumentCaptor<String>()
                        val version = "1.0.0"

                        subject!!.versionExists(version)

                        it("Should prepare the statement") {
                            verify(connection)!!.prepareStatement(statementCaptor.capture())
                            assertEquals(expectedStatement, statementCaptor.value)
                        }

                        it("Should record populate the statement with the value of the version") {
                            verify(preparedStatement)!!.setString(1, version)
                        }

                        it("Should execute the statement on the database") {
                            verify(preparedStatement)!!.executeQuery()
                        }
                    }

                    on("Checking a task exists") {
                        val expectedStatement = """SELECT * FROM change_set
                        WHERE version = ?
                            AND task = ?
                     """
                        val statementCaptor = argumentCaptor<String>()
                        val version = "1.0.0"
                        val taskName = "Task Name"

                        subject!!.taskExists(version, taskName)

                        it("Should prepare the statement") {
                            verify(connection)!!.prepareStatement(statementCaptor.capture())
                            assertEquals(expectedStatement, statementCaptor.value)
                        }

                        it("Should record populate the statement with the value of the version") {
                            verify(preparedStatement)!!.setString(1, version)
                        }

                        it("Should record populate the statement with the value of the task name") {
                            verify(preparedStatement)!!.setString(2, taskName)
                        }

                        it("Should execute the statement") {
                            verify(preparedStatement)!!.executeQuery()
                        }
                    }
                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null
                    val statementCaptor = argumentCaptor<String>()

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)

                        whenever(metaData!!.getTables(null, null, "change_set", null))
                                .thenReturn(ResultsSetStub(false))
                        whenever(metaData!!.getColumns(null, null, "change_set", "hash"))
                                .thenReturn(ResultsSetStub(false))

                    }

                    on("checking the existence of the change set table") {

                        subject!!.checkChangeSetTable(true)

                        it("should check the metadata for the tables existence") {
                            verify(metaData)!!.getTables(null, null, "change_set", null)
                        }

                        it("should execute create and update statements") {
                            verify(statement, times(2))!!.execute(statementCaptor.capture())
                        }

                        it("should create the change set table") {
                            val expectedStatement = """CREATE TABLE change_set (
                                            version VARCHAR(255),
                                            task VARCHAR(255),
                                            applied DATE
                                          )"""
                            assertEquals(expectedStatement, statementCaptor.allValues[0])
                        }

                        it("should check the metadata for the hash columns existence") {
                            verify(metaData)!!.getColumns(null, null, "change_set", "hash")
                        }

                        it("should create the hash column") {
                            val expectedStatement = "ALTER TABLE change_set ADD COLUMN hash VARCHAR(64)"
                            assertEquals(expectedStatement, statementCaptor.allValues[1])
                        }

                        it("should commit the changes to the database") {
                            verify(connection, times(2))!!.commit()
                        }
                    }
                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)

                        whenever(metaData!!.getTables(null, null, "change_set", null))
                                .thenReturn(ResultsSetStub(true))
                        whenever(metaData!!.getColumns(null, null, "change_set", "hash"))
                                .thenReturn(ResultsSetStub(false))

                    }

                    on("checking the existence of the change set table") {

                        subject!!.checkChangeSetTable(true)

                        it("Should check for the change set table") {
                            verify(metaData)!!.getTables(null, null, "change_set", null)
                        }

                        it("Should check for the hash columns existence in the table") {
                            verify(metaData)!!.getColumns(null, null, "change_set", "hash")
                        }

                        it("Should update the table schema for the hash column") {
                            val expectedStatement = "ALTER TABLE change_set ADD COLUMN hash VARCHAR(64)"
                            verify(statement)!!.execute(check {
                                assertEquals(expectedStatement, it)
                            })
                        }

                        it("should commit the changes to the database") {
                            verify(connection)!!.commit()
                        }

                    }
                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)

                        whenever(metaData!!.getTables(null, null, "change_set", null)).thenReturn(ResultsSetStub(true))
                        whenever(metaData!!.getColumns(null, null, "change_set", "hash")).thenReturn(ResultsSetStub(true))

                    }

                    on("checking the existence of the change set table") {

                         subject!!.checkChangeSetTable(true)

                        it("Should check for the change set table") {
                            verify(metaData)!!.getTables(null, null, "change_set", null)
                        }

                        it("Should check for the hash columns existence in the table") {
                            verify(metaData)!!.getColumns(null, null, "change_set", "hash")
                        }

                        it("Should not create the table or update the table schema for the hash column") {
                            verify(statement, never())!!.execute(any())
                        }

                        it("should not commit the changes to the database") {
                            verify(connection, never())!!.commit()
                        }

                    }

                    on("checking a change sets hash column matches") {
                        val expectedStatement = """SELECT * FROM change_set
                        WHERE version = ?
                            AND task = ?
                            AND hash = ?
                     """
                        val statementCaptor = argumentCaptor<String>()
                        val version = "1.0.0"
                        val taskName = "Task Name"
                        val query = "SELECT * FROM Table"
                        val result = subject!!.taskHashMatches(version, taskName, query)

                        it("Should prepare the statement") {
                            verify(connection)!!.prepareStatement(statementCaptor.capture())
                            assertEquals(expectedStatement, statementCaptor.value)
                        }

                        it("Should record populate the statement with the value of the version") {
                            verify(preparedStatement)!!.setString(1, version)
                        }

                        it("Should record populate the statement with the value of the task name") {
                            verify(preparedStatement)!!.setString(2, taskName)
                        }

                        it("Should record populate the statement with the value of the query") {
                            verify(preparedStatement)!!.setString(3, query.toMD5())
                        }

                        it("Should execute the statement") {
                            verify(preparedStatement)!!.executeQuery()
                        }

                        it("Should return true") {
                            assertTrue(result)
                        }

                    }
                }

                given("A Database Connector") {

                    var subject: DefaultDatabaseConnector? = null

                    beforeEachTest {
                        subject = DefaultDatabaseConnector(connection!!)

                        whenever(preparedStatement!!.executeQuery()).thenReturn(ResultsSetStub(false))

                    }

                    on("checking a change sets hash column doesn't match") {
                        val expectedStatement = """SELECT * FROM change_set
                        WHERE version = ?
                            AND task = ?
                            AND hash = ?
                     """
                        val statementCaptor = argumentCaptor<String>()
                        val version = "1.0.0"
                        val taskName = "Task Name"
                        val query = "SELECT * FROM Table"
                        val result = subject!!.taskHashMatches(version, taskName, query)

                        it("Should prepare the statement") {
                            verify(connection)!!.prepareStatement(statementCaptor.capture())
                            assertEquals(expectedStatement, statementCaptor.value)
                        }

                        it("Should record populate the statement with the value of the version") {
                            verify(preparedStatement)!!.setString(1, version)
                        }

                        it("Should record populate the statement with the value of the task name") {
                            verify(preparedStatement)!!.setString(2, taskName)
                        }

                        it("Should record populate the statement with the value of the query") {
                            verify(preparedStatement)!!.setString(3, query.toMD5())
                        }

                        it("Should execute the statement") {
                            verify(preparedStatement)!!.executeQuery()
                        }

                        it("Should return true") {
                            assertFalse(result)
                        }

                    }
                }
            }

        })
