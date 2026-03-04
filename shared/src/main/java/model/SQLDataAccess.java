package dataAccess;

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

  protected int executeUpdate(String statement, Object... params) throws ResponseException {
    try (var conn = DatabaseManager.getConnection()) {
      try (var ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS)) {
        for (var i = 0; i < params.length; i++) {
          var param = params[i];
          if (param instanceof String p) ps.setString(i + 1, p);
          else if (param instanceof Integer p) ps.setInt(i + 1, p);
          else if (param == null) {
            System.out.println("Setting param (" + (i + 1) + ") to NULL...");
            ps.setNull(i + 1, NULL);
          }
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
