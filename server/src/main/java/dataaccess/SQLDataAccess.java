package dataaccess;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.sql.Types.NULL;

import exception.ResponseException;
import java.sql.*;

abstract class SQLDataAccess {
    public SQLDataAccess(final String[] createStatements) throws ResponseException {
        configureDatabase(createStatements);
    }

    private void configureDatabase(final String[] createStatements) throws ResponseException {
        System.out.println("Configuring database...");
        try {
            DatabaseManager.createDatabase();
            try (var conn = DatabaseManager.getConnection()) {
                System.out.println("Creating tables...");
                for (var statement : createStatements) {
                    try (var preparedStatement = conn.prepareStatement(statement)) {
                        System.out.println("Executing: " + statement);
                        preparedStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            throw new ResponseException(
                    500, String.format("Unable to configure database: %s", ex.getMessage()));
        } catch (DataAccessException ex) {
            throw new ResponseException(
                    500, String.format("Unable to configure database: %s", ex.getMessage()));
        }
    }

    private void setParam(PreparedStatement ps, int index, Object param) throws SQLException {
        if (param instanceof String p) {
            ps.setString(index, p);
        } else if (param instanceof Integer p) {
            ps.setInt(index, p);
        } else if (param == null) {
            System.out.println("Setting param (" + index + ") to NULL...");
            ps.setNull(index, NULL);
        }
    }

    protected int executeUpdate(String statement, Object... params) throws ResponseException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS)) {
                for (var i = 0; i < params.length; i++) {
                    setParam(ps, i + 1, params[i]);
                }
                System.out.println("Executing: " + ps.toString());
                ps.executeUpdate();

                var rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }

                return 0;
            }
        } catch (SQLException e) {
            throw new ResponseException(
                    500, String.format("unable to update database: %s, %s", statement, e.getMessage()));
        } catch (DataAccessException e) {
            throw new ResponseException(
                    500, String.format("unable to update database: %s, %s", statement, e.getMessage()));
        }
    }
}
