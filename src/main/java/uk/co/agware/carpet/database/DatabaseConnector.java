package uk.co.agware.carpet.database;

import uk.co.agware.carpet.exception.MagicCarpetException;

import java.sql.Connection;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 7/05/2016.
 */
public interface DatabaseConnector {

    void setConnection(String jdbcName);

    void setConnection(String connectionUrl, String name, String password);

    void setConnection(Connection connection);

    boolean commit();

    boolean close();

    boolean insertChange(String version, String taskName);

    boolean executeStatement(String sql);

    void checkChangeSetTable(boolean createTable) throws MagicCarpetException;

    boolean changeExists(String version, String taskName);

    void rollBack();
}