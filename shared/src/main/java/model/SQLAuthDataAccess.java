package dataAccess;

import exception.ResponseException;
import model.AuthData;

public class SQLAuthDataAccess extends SQLDataAccess implements AuthDataAccess {
  private static final String[] createStatements = {
    """
    CREATE TABLE IF NOT EXISTS auth (
      username varchar(256) NOT NULL,
      authToken varchar(256) NOT NULL,
      PRIMARY KEY (authToken),
      FOREIGN KEY (username) REFERENCES user(username)
    );
    """
  };

  public SQLAuthDataAccess() throws ResponseException {
    super(createStatements);
  }

  // Clear all auths.
  public void clear() throws ResponseException {
    var statement = "DELETE FROM auth";
    executeUpdate(statement);
  }

  // Create a new authorization.
  public AuthData createAuth(AuthData auth) throws ResponseException {
    var statement = "INSERT INTO auth (username, authToken) VALUES (?, ?)";
    executeUpdate(statement, auth.getUsername(), auth.getAuthToken());
    return auth;
  }

  // Retrieve an authorization given an authToken.
  public AuthData getAuth(String authToken) throws ResponseException {
    try (var conn = DatabaseManager.getConnection()) {
      var statement = "SELECT username, authToken FROM auth WHERE authToken = ?";
      try (var ps = conn.prepareStatement(statement)) {
        ps.setString(1, authToken);
        try (var rs = ps.executeQuery()) {
          if (rs.next()) {
            return new AuthData(rs.getString("username"), rs.getString("authToken"));
          }
        }
      }
    } catch (Exception e) {
      throw new ResponseException(500, String.format("Unable to read data: %s", e.getMessage()));
    }
    return null;
  }

  // Delete an authorization so that it is no longer valid.
  public void deleteAuth(String authToken) throws ResponseException {
    var statement = "DELETE FROM auth WHERE authToken = ?";
    executeUpdate(statement, authToken);
  }
}
