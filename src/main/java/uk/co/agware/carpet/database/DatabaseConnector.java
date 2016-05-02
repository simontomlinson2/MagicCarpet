package uk.co.agware.carpet.database;

import uk.co.agware.carpet.change.Change;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 26/02/2016.
 */
public class DatabaseConnector {

    public static final String TABLE_NAME = "change_set";
    public static final String CHANGE_COLUMN = "change";
    public static final String DATE_COLUMN = "applied";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnector.class);

    private Connection connection;

    public boolean setConnection(String jdbcName){
        try {
            DataSource source = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/" + jdbcName);
            connection = source.getConnection();
            connection.setAutoCommit(false);
            return true;
        } catch (NamingException | SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean setConnection(String connectionUrl, String name, String password){
        try {
            connection = DriverManager.getConnection(connectionUrl, name, password);
            connection.setAutoCommit(false);
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean setConnection(Connection connection){
        try {
            this.connection = connection;
            this.connection.setAutoCommit(false);
            return true;
        }
        catch (SQLException e){
            LOGGER.error(e.getMessage(), e);
            return true;
        }
    }

    public boolean commit(){
        try {
            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean close(){
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean executeStatement(String sql){
        try(Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean insertChange(String value){
        String sql = "INSERT INTO " +TABLE_NAME +"(" +CHANGE_COLUMN + "," +DATE_COLUMN +") VALUES(?,?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setString(1, value);
            preparedStatement.setDate(2, new Date(System.currentTimeMillis()));
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean checkChangeSetTable(){
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, TABLE_NAME, null);
            if(!tables.next()){
                Statement statement = connection.createStatement();
                String createTableStatement = "CREATE TABLE " +TABLE_NAME + " (" +
                        CHANGE_COLUMN +" varchar(255), " +
                        DATE_COLUMN +" date)";
                statement.executeUpdate(createTableStatement);
                return commit();
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean checkChangeExists(String changeNumber){
        String query = "SELECT * FROM " +TABLE_NAME + " WHERE " +CHANGE_COLUMN + " = ?";
        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, changeNumber);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean executeChanges(List<Change> changes){
        boolean allSuccess = true;
        for(Change c : changes){
            allSuccess &= executeChange(c);
        }
        if(allSuccess){
            if(commit()){
                return close();
            }
            else {
                try {
                    connection.rollback();
                    LOGGER.error("Failures while applying changes, performing a rollback");
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        else {
            try {
                connection.rollback();
                LOGGER.error("Failures while applying changes, performing a rollback");
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return allSuccess;
    }

    public boolean executeChange(Change change){
        if(checkChangeExists(change.getId())) return true;
        LOGGER.info("Executing Update " + change.getId());
        boolean allSuccess = true;
        for(String s : change.getInputList()){
            allSuccess &= executeStatement(s);
        }
        if(allSuccess){
            insertChange(change.getId());
        }
        return allSuccess;
    }
}
