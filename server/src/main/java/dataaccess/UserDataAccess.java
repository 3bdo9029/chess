package dataaccess;

import exception.ResponseException;
import model.UserData;

public interface UserDataAccess extends DataAccess {
    // Create a new user.
    public UserData createUser(UserData user) throws ResponseException;

    // Retrieve a user with the given username.
    public UserData getUser(String username) throws ResponseException;
}
