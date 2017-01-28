package uk.co.agware.carpet

import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.database.DatabaseConnector
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class TestFileTask: SubjectSpek<FileTask>({

    given("A database connection") {
        val connection = Mockito.mock(DatabaseConnector::class.java)

        describe("A FileTask object") {

            subject { FileTask("Test Task", 1, "src\\test\\files\\test.sql") }

            on("Performing the task") {

                subject.performTask(connection)

                it("should execute each statement") {
                    val statements = ArgumentCaptor.forClass(String::class.java)

                    Mockito.verify<DatabaseConnector>(connection,
                                                      Mockito.times(2)).executeStatement(statements.capture())
                    assertEquals(2, statements.allValues.size)
                    assertTrue(statements.allValues.contains("SELECT * FROM Table"))
                    assertTrue(statements.allValues.contains("SELECT * FROM Other_Table"))
                }

            }


        }

        describe("A FileTask Object from the classpath") {

            subject { FileTask("Class Path FileTask", 1, "classpath:classpathTest.sql", ",") }

            on("Performing the task") {

                subject.performTask(connection)

                it("should execute each statement") {
                    val statements = ArgumentCaptor.forClass(String::class.java)

                    Mockito.verify<DatabaseConnector>(connection,
                                                      Mockito.times(2)).executeStatement(statements.capture())
                    assertEquals(2, statements.allValues.size)
                    assertTrue(statements.allValues.contains("SELECT * FROM Table"))
                    assertTrue(statements.allValues.contains("SELECT * FROM Other_Table"))
                }


            }
        }
    }


 })
