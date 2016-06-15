package uk.co.agware.carpet.stubs;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 14/06/2016.
 *
 * Used to trick the DriverManager into creating a connection for us
 */
public class JdbcStub implements Driver {

    static {
        try {
            Driver driver = new JdbcStub();
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return new ConnectionStub();
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:test");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
