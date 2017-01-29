package uk.co.agware.carpet

import com.nhaarman.mockito_kotlin.argumentCaptor
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import uk.co.agware.carpet.change.tasks.ScriptTask
import uk.co.agware.carpet.database.DatabaseConnector
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class TestScriptTask: Spek({

   describe("A ScriptTask object") {

        val subject = ScriptTask("Test Task", 1, "SELECT * FROM Table; SELECT * FROM Other_Table")

       given("A database connection") {
            val connection = Mockito.mock(DatabaseConnector::class.java)

            on("Performing the task") {

                subject.performTask(connection)

                it("should execute each statement") {
                    val statement =  argumentCaptor<String>()
                    Mockito.verify<DatabaseConnector>(connection,
                                                        Mockito.times(2)).executeStatement(statement.capture())
                    assertEquals(2, statement.allValues.size)
                    assertTrue(statement.allValues.contains("SELECT * FROM Table"))
                    assertTrue(statement.allValues.contains("SELECT * FROM Other_Table"))

                }

            }


       }

   }

    describe("A ScriptTask object with delimiter"){

        val subject = ScriptTask("Test Task", 1, "SELECT * FROM Table, SELECT * FROM Other_Table", ",")

        given("A database connection") {
            val connection = Mockito.mock(DatabaseConnector::class.java)

            on("Performing the task") {

                subject.performTask(connection)

                it("should execute each statement") {
                    val statement =  argumentCaptor<String>()
                    Mockito.verify<DatabaseConnector>(connection,
                                                      Mockito.times(2)).executeStatement(statement.capture())
                    assertEquals(2, statement.allValues.size)
                    assertTrue(statement.allValues.contains("SELECT * FROM Table"))
                    assertTrue(statement.allValues.contains("SELECT * FROM Other_Table"))

                }

            }


        }
    }


})
