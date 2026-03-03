package dataAccess;

import exception.ResponseException;
import model.AuthData;

public interface AuthDataAccess extends DataAccess {
    // Create a new authorization.
    public AuthData createAuth(AuthData auth) throws ResponseException;

    // Retrieve an authorization given an authToken.
    public AuthData getAuth(String authToken) throws ResponseException;

    // Delete an authorization so that it is no longer valid.
    public void deleteAuth(String authToken) throws ResponseException;
}
