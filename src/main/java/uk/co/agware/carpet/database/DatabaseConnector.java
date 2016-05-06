package uk.co.agware.carpet.database;

import uk.co.agware.carpet.change.Change;

import java.sql.Connection;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 7/05/2016.
 */
public interface DatabaseConnector {

    boolean setConnection(String jdbcName);

    boolean setConnection(String connectionUrl, String name, String password);

    boolean setConnection(Connection connection);

    boolean commit();

    boolean close();

    boolean executeStatement(String sql);

    boolean checkChangeSetTable();

    boolean checkChangeExists(String changeNumber);

    boolean executeChanges(List<Change> changes);

    boolean executeChange(Change change);
}
