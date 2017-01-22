package uk.co.agware.carpet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import uk.co.agware.carpet.database.DatabaseConnector;
import uk.co.agware.carpet.database.DefaultDatabaseConnector;
import uk.co.agware.carpet.stubs.ResultsSetStub;

import java.sql.*;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 23/04/2016.
 */
//TODO Need to add some negative tests into these
    //TODO need to spoof the initial context so I can test the JDBC lookup properly
public class TestDatabaseConnector {

    private DatabaseConnector databaseConnector;
    private Connection connection;
    private Statement statement;
    private PreparedStatement preparedStatement;

    @Before
    public void buildMocks() throws SQLException {
        databaseConnector = new DefaultDatabaseConnector();
        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(Statement.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.execute()).thenReturn(true);
        Mockito.when(statement.execute(Mockito.anyString())).thenReturn(true);

        databaseConnector.setConnection(connection);
    }

    @Test
    public void testCommit() throws SQLException {
        databaseConnector.commit();
        Mockito.verify(connection).commit();
    }

    @Test
    public void testClose() throws SQLException {
        databaseConnector.close();
        Mockito.verify(connection).close();
    }

    @Test
    public void testExecuteStatement() throws SQLException {
        databaseConnector.executeStatement("SELECT * FROM Table");
        Mockito.verify(statement).execute("SELECT * FROM Table");
    }

    @Test
    public void testInsertChange() throws SQLException {
        databaseConnector.insertChange("1.0.0", "Create DB", "SELECT * FROM Table");
        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection).prepareStatement(statement.capture());
        Mockito.verify(preparedStatement).execute();
        String expectedStatement = "INSERT INTO change_set (version,task,query_hash,applied) VALUES (?,?,?,?)";
        Assert.assertEquals(expectedStatement,statement.getValue());
        Mockito.verify(preparedStatement).setString(1, "1.0.0");
        Mockito.verify(preparedStatement).setString(2, "Create DB");
        Mockito.verify(preparedStatement).setInt(3, "SELECT * FROM Table".hashCode());
        Mockito.verify(preparedStatement).setDate(Mockito.anyInt(), Mockito.anyObject());
    }

    @Test
    public void testCheckChangeSetTable() throws SQLException {
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getTables(null, null, "change_set", null)).thenReturn(new ResultsSetStub(false)); // Returns a results set which returns false for the .next() method
        databaseConnector.checkChangeSetTable(true);
        Mockito.verify(connection).getMetaData();
        Mockito.verify(metaData).getTables(null, null, "change_set", null);
        ArgumentCaptor<String> updateStatement = ArgumentCaptor.forClass(String.class);
        Mockito.verify(statement).executeUpdate(updateStatement.capture());
        String expectedSql = "CREATE TABLE change_set (version VARCHAR(255), task VARCHAR(255), query_hash BIGINT, applied DATE)";
        Assert.assertEquals(expectedSql, updateStatement.getValue());
    }

    @Test
    public void testCheckChangeSetTableAlreadyExists() throws SQLException {
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getTables(null, null, "change_set", null)).thenReturn(new ResultsSetStub(true));
        Mockito.when(metaData.getColumns(null, null, "change_set", "query_hash")).thenReturn(new ResultsSetStub(true));
        databaseConnector.checkChangeSetTable(true);
        Mockito.verify(connection, Mockito.times(2)).getMetaData();
        Mockito.verify(metaData).getTables(null, null, "change_set", null);
        Mockito.verify(metaData).getColumns(null, null, "change_set", "query_hash");
        Mockito.verify(statement, Mockito.times(0)).executeUpdate(Mockito.anyString()); // Ensure the create statement wasn't run
    }

    @Test
    public void checkChangeDoesntExist() throws SQLException {
        Mockito.when(preparedStatement.executeQuery()).thenReturn(new ResultsSetStub(false));
        Assert.assertFalse(databaseConnector.changeExists("1.0.0", "Create DB", "SELECT * FROM Table"));
        String expectedSql = "SELECT * FROM change_set WHERE version = ? AND task = ? AND (query_hash = ? OR query_hash IS NULL)";
        Mockito.verify(connection).prepareStatement(expectedSql);
        Mockito.verify(preparedStatement).setString(1, "1.0.0");
        Mockito.verify(preparedStatement).setString(2, "Create DB");
        Mockito.verify(preparedStatement).setInt(3, "SELECT * FROM Table".hashCode());
    }

    @Test
    public void checkChangeExists() throws SQLException {
        Mockito.when(preparedStatement.executeQuery()).thenReturn(new ResultsSetStub(true));
        Assert.assertTrue(databaseConnector.changeExists("1.0.0", "Create DB", "Select * FROM Table"));
    }

    @Test
    public void testRollBack() throws SQLException {
        databaseConnector.rollBack();
        Mockito.verify(connection).rollback();
    }
}
