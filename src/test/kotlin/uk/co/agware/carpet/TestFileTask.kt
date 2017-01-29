package uk.co.agware.carpet

import com.nhaarman.mockito_kotlin.argumentCaptor
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.database.DatabaseConnector
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class TestFileTask: SubjectSpek<FileTask>({

        describe("A FileTask Object from the classpath") {

            subject { FileTask("Test Task", 1, "classpath:classpathTest.sql", ",") }

            given("A database connection") {
                val connection = Mockito.mock(DatabaseConnector::class.java)


                on("Performing the task") {

                    subject.performTask(connection)

                    it("should execute each statement") {
                        argumentCaptor<String>().apply {
                            Mockito.verify<DatabaseConnector>(connection,
                                                              Mockito.times(2)).executeStatement(capture())
                            assertEquals(2, allValues.size)
                            assertTrue(allValues.contains("SELECT * FROM Classpath"))
                            assertTrue(allValues.contains("SELECT * FROM class_path"))
                        }

                    }

                }


            }
        }

        describe("A FileTask object") {

            subject { FileTask("Test Task", 1, "src/test/files/test.sql") }

            given("A database connection") {
                val connection = Mockito.mock(DatabaseConnector::class.java)

                on("Performing the task") {

                    subject.performTask(connection)

                    it("should execute each statement") {
                        argumentCaptor<String>().apply {
                            Mockito.verify<DatabaseConnector>(connection,
                                                              Mockito.times(2)).executeStatement(capture())
                            assertEquals(2, allValues.size)
                            assertTrue(allValues.contains("SELECT * FROM Table"))
                            assertTrue(allValues.contains("SELECT * FROM Other_Table"))
                        }

                    }

                }


            }

        }


})
