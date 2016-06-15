package uk.co.agware.carpet.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.carpet.exception.MagicCarpetException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 26/02/2016.
 */
public class DefaultDatabaseConnector implements DatabaseConnector {

    private static final String TABLE_NAME = "change_set";
    private static final String VERSION_COLUMN = "version";
    private static final String TASK_COLUMN = "task";
    private static final String DATE_COLUMN = "applied";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDatabaseConnector.class);

    private Connection connection;

    @Override
    public void setConnection(String jdbcName) {
        try {
            DataSource source = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/" + jdbcName);
            connection = source.getConnection();
            connection.setAutoCommit(false);
        } catch (NamingException | SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e.getMessage(), e);
        }
    }

    @Override
    public void setConnection(String connectionUrl, String name, String password) {
        try {
            connection = DriverManager.getConnection(connectionUrl, name, password);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e.getMessage(), e);
        }
    }

    @Override
    public void setConnection(Connection connection) {
        try {
            this.connection = connection;
            this.connection.setAutoCommit(false);
        }
        catch (SQLException e){
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e.getMessage(), e);
        }
    }

    @Override
    public boolean commit(){
        try {
            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean close(){
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean executeStatement(String sql){
        try(Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean insertChange(String version, String taskName){
        String sql = String.format("INSERT INTO %s (%s,%s,%s) VALUES (?,?,?)", TABLE_NAME, VERSION_COLUMN, TASK_COLUMN, DATE_COLUMN);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setString(1, version);
            preparedStatement.setString(2, taskName);
            preparedStatement.setDate(3, new Date(System.currentTimeMillis()));
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void checkChangeSetTable() {
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, TABLE_NAME, null);
            if(!tables.next()){
                Statement statement = connection.createStatement();
                String createTableStatement = "CREATE TABLE " +TABLE_NAME + " (" +
                        VERSION_COLUMN +" VARCHAR(255), " +
                        TASK_COLUMN +" VARCHAR(255), " +
                        DATE_COLUMN +" DATE)";
                statement.executeUpdate(createTableStatement);
                commit();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e.getMessage(), e);
        }
    }

    @Override
    public boolean changeExists(String changeVersion, String taskName){
        String query = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", TABLE_NAME, VERSION_COLUMN, TASK_COLUMN);
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, changeVersion);
            statement.setString(2, taskName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void rollBack() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
