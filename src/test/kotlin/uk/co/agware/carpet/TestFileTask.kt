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
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetParseException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class TestFileTask: Spek({

        describe("A FileTask Object from the classpath") {

            val subject = FileTask("Test Task", 1, "classpath:classpathTest.sql", ",")

            given("A database connection") {
                val connection = Mockito.mock(DatabaseConnector::class.java)


                on("Performing the task") {

                    subject.performTask(connection)

                    it("should execute each statement") {
                        val statement =  argumentCaptor<String>()
                        Mockito.verify<DatabaseConnector>(connection,
                                                              Mockito.times(2)).executeStatement(statement.capture())
                        assertEquals(2, statement.allValues.size)
                        assertTrue(statement.allValues.contains("SELECT * FROM Classpath"))
                        assertTrue(statement.allValues.contains("SELECT * FROM class_path"))

                    }

                }


            }
        }

        describe("A FileTask object") {

            val subject = FileTask("Test Task", 1, "src/test/files/test.sql")

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

    describe("A fileTask object with a file that doesn't exist"){

        it("Should fail with a MagicCarpetParseException"){
            assertFailsWith<MagicCarpetParseException> { FileTask("A Failing Task", 2, "this/doesnt/exist") }
        }
    }

    describe("A fileTask object with a file that doesn't exist on the classpath"){

        it("Should fail with a MagicCarpetParseException"){
            assertFailsWith<MagicCarpetParseException> { FileTask("A Failing Task", 2, "classpath:this.doesnt.exist") }
        }
    }


})
