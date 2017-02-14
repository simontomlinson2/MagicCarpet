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

// TODO Improve the test descriptions
// TODO Test what happens with empty files
@RunWith(JUnitPlatform::class)
class TestFileTask: Spek({

  describe("A FileTask Object") {

    var connection = Mockito.mock(DatabaseConnector::class.java)

    beforeEachTest {
      connection = Mockito.mock(DatabaseConnector::class.java)
    }

    given("A classpath file") {

      val task = FileTask("Test Task", 1, "classpath:classpathTest.sql", ",")

      on("task execution") {

        task.performTask(connection)

        it("should execute each statement") {
          val statements = argumentCaptor<String>()
          Mockito.verify<DatabaseConnector>(connection, Mockito.times(2))
                 .executeStatement(statements.capture())

          assertEquals(2, statements.allValues.size)
          assertTrue(statements.allValues.contains("SELECT * FROM Classpath"))
          assertTrue(statements.allValues.contains("SELECT * FROM class_path"))
        }
      }

      it("Should fail with a MagicCarpetParseException") {
        assertFailsWith<MagicCarpetParseException> {
          FileTask("A Failing Task", 2, "classpath:this.does.not.exist")
        }
      }
    }

    given("A file system file") {

      val subject = FileTask("Test Task", 1, "src/test/files/test.sql")

      on("task execution") {

        subject.performTask(connection)

        it("should execute each statement") {
          val statements = argumentCaptor<String>()
          Mockito.verify<DatabaseConnector>(connection, Mockito.times(2))
                 .executeStatement(statements.capture())

          assertEquals(2, statements.allValues.size)
          assertTrue(statements.allValues.contains("SELECT * FROM Table"))
          assertTrue(statements.allValues.contains("SELECT * FROM Other_Table"))
        }
      }

      it("Should fail with a MagicCarpetParseException") {
        assertFailsWith<MagicCarpetParseException> {
          FileTask("A Failing Task", 2, "this/does/not/exist")
        }
      }
    }
  }
})
