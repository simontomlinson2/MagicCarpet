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
import uk.co.agware.carpet.stubs.ResultsSetStub
import java.nio.file.Paths
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import kotlin.test.assertEquals
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

      given("A directory structure") {
        // TODO Shadowed variable, watch the linter, its basically telling you that you messed up your brackets
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
    }
  }
})
