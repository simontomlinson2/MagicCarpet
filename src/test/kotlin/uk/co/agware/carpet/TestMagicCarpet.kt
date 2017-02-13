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
import uk.co.agware.carpet.exception.MagicCarpetParseException
import uk.co.agware.carpet.stubs.ResultsSetStub
import java.nio.file.Paths
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class TestMagicCarpet: Spek({

  describe("A MagicCarpet Object") {

    var connector = mock<DefaultDatabaseConnector>()
    var metaData = mock<DatabaseMetaData>()
    var preparedStatement = mock<PreparedStatement>()
    var subject = MagicCarpet(connector)

    beforeEachTest {
      connector = mock<DefaultDatabaseConnector>()
      metaData = mock<DatabaseMetaData>()
      preparedStatement = mock<PreparedStatement>()

      whenever(preparedStatement.execute()).thenReturn(true)
      whenever(metaData.getTables(null, null, "change_set", null))
              .thenReturn(ResultsSetStub(true))
      whenever(metaData.getColumns(null, null, "change_set", "hash"))
              .thenReturn(ResultsSetStub(true))
    }

    given("A base xml file path") {

      val path = Paths.get("src/test/files/ChangeSet.xml")

      beforeEachTest {
        subject = MagicCarpet(connector, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(false)
      }

      on("Parsing Changes") {

        subject.parseChanges()

        it("Should read the xml changes") {
          assertEquals(2, subject.changes.size)
          assertEquals(1, subject.changes[0].tasks.size)
        }
      }

      on("Executing changes") {

        val statementCaptor = argumentCaptor<String>()
        subject.run()

        it("should check the change set table exists") {
          verify(connector).checkChangeSetTable(any())
        }

        it("should check the version exists in the database") {
          verify(connector, times(2)).versionExists(any())
        }

        it("should perform the tasks") {
          verify(connector, times(5)).executeStatement(statementCaptor.capture())
          assertEquals(5, statementCaptor.allValues.size)
          assertTrue {
            statementCaptor.allValues.contains("create table test(version integer, test date)")
            statementCaptor.allValues.contains("alter table test add column another varchar(64)")
            statementCaptor.allValues.contains("create table second(version varchar(64))")
            statementCaptor.allValues.contains("SELECT * FROM Table")
            statementCaptor.allValues.contains("SELECT * FROM Other_Table")
          }
        }

        it("should record the tasks") {
          verify(connector, times(2)).recordTask(any(), any(), any())
        }
      }
    }

    given("A base json file path") {

      val path = Paths.get("src/test/files/ChangeSet.json")

      beforeEachTest {
        subject = MagicCarpet(connector, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(false)
      }

      on("Parsing Changes") {

        subject.parseChanges()

        it("Should read the json changes") {
          assertEquals(2, subject.changes.size)
          assertEquals(1, subject.changes[0].tasks.size)
        }
      }

      on("Executing changes") {

        val statementCaptor = argumentCaptor<String>()
        subject.run()

        it("should check the change set table exists") {
          verify(connector).checkChangeSetTable(any())
        }

        it("should check the version exists in the database") {
          verify(connector, times(2)).versionExists(any())
        }

        it("should perform the tasks") {
          verify(connector, times(5)).executeStatement(statementCaptor.capture())
          assertEquals(5, statementCaptor.allValues.size)
          assertTrue {
            statementCaptor.allValues.contains("create table test(version integer, test date)")
            statementCaptor.allValues.contains("alter table test add column another varchar(64)")
            statementCaptor.allValues.contains("create table second(version varchar(64))")
            statementCaptor.allValues.contains("SELECT * FROM Table")
            statementCaptor.allValues.contains("SELECT * FROM Other_Table")
          }
        }

        it("should record the tasks") {
          verify(connector, times(2)).recordTask(any(), any(), any())
        }
      }
    }

    given("A directory structure") {
      val path = Paths.get("src/test/files/nest")

      beforeEachTest {
        subject = MagicCarpet(connector, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(false)
      }

      on("Parsing Changes") {

        subject.parseChanges()

        it("Should read the xml changes") {
          assertEquals(3, subject.changes.size)
          assertEquals(1, subject.changes[0].tasks.size)
        }
      }

      on("Executing changes") {

        val statementCaptor = argumentCaptor<String>()
        subject.run()

        it("should check the change set table exists") {
          verify(connector).checkChangeSetTable(any())
        }

        it("should check the version exists in the database") {
          verify(connector, times(3)).versionExists(any())
        }

        it("should perform the tasks") {
          verify(connector, times(6)).executeStatement(statementCaptor.capture())
          assertEquals(6, statementCaptor.allValues.size)
          assertTrue {
            statementCaptor.allValues.contains("create table test(version integer, test date)")
            statementCaptor.allValues.contains("alter table test add column another varchar(64)")
            statementCaptor.allValues.contains("create table second(version varchar(64))")
            statementCaptor.allValues.contains("create table third(version varchar(64))")
            statementCaptor.allValues.contains("SELECT * FROM Table")
            statementCaptor.allValues.contains("SELECT * FROM Other_Table")
          }
        }

        it("should record the tasks") {
          verify(connector, times(6)).recordTask(any(), any(), any())
        }
      }
    }

    given("A File that doesn't exist") {

      val path = Paths.get("this/does/not/exist")

      beforeEachTest {
        subject = MagicCarpet(connector, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(false)
      }

      it("should fail to parse the changes") {
        assertFailsWith<MagicCarpetParseException>{
          subject.parseChanges()
        }

      }
    }

    given("A base json file path and developer mode on") {

      val path = Paths.get("src/test/files/ChangeSet.json")

      beforeEachTest {
        subject = MagicCarpet(connector, devMode = true, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(false)
      }

      on("Parsing Changes") {

        subject.parseChanges()

        it("Should not read the json changes") {
          assertEquals(0, subject.changes.size)
        }
      }

      on("Executing changes") {

        val statementCaptor = argumentCaptor<String>()
        subject.executeChanges()

        it("should not check the change set table exists") {
          verify(connector, never()).checkChangeSetTable(any())
        }

        it("should not check the version exists in the database") {
          verify(connector, never()).versionExists(any())
        }

        it("should not  perform the tasks") {
          verify(connector, never()).executeStatement(statementCaptor.capture())
          assertEquals(0, statementCaptor.allValues.size)

        }

        it("should not record the tasks") {
          verify(connector, never()).recordTask(any(), any(), any())
        }
      }
    }

    given("A base json file path with changes that have already been run") {

      val path = Paths.get("src/test/files/ChangeSet.json")

      beforeEachTest {
        subject = MagicCarpet(connector, basePath = path)
        whenever(connector.versionExists(any())).thenReturn(true)
        whenever(connector.taskExists(any(), any())).thenReturn(true)
        whenever(connector.taskHashMatches(any(), any(), any())).thenReturn(false)
      }

      on("Parsing Changes") {

        subject.parseChanges()

        it("Should read the json changes") {
          assertEquals(2, subject.changes.size)
          assertEquals(1, subject.changes[0].tasks.size)
        }
      }

      on("Executing changes") {

        subject.run()

        it("should check the change set table exists") {
          verify(connector).checkChangeSetTable(any())
        }

        it("should check the version exists in the database") {
          verify(connector, times(2)).versionExists(any())
        }

        it("should check the task exists"){
          verify(connector, times(2)).taskExists(any(), any())
        }

        it("should update the task hash"){
          verify(connector, times(2)).updateTaskHash(any(), any(), any())
        }

        it("should not  perform the tasks") {
            val statementCaptor = argumentCaptor<String>()
            verify(connector, never()).executeStatement(statementCaptor.capture())
            assertEquals(0, statementCaptor.allValues.size)
        }

        it("should not record the tasks") {
          verify(connector, never()).recordTask(any(), any(), any())
        }

      }
    }

  }
})
