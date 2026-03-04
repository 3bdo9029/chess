package service;

import dataaccess.*;
import exception.ResponseException;
import model.*;
import server.*;

public class UserService {
    private UserDataAccess userDataAccess;
    private AuthDataAccess authDataAccess;

    public UserService(UserDataAccess userDataAccess, AuthDataAccess authDataAccess) {
        this.userDataAccess = userDataAccess;
        this.authDataAccess = authDataAccess;
    }

    public AuthData registerUser(UserData user) throws ResponseException {
        System.out.println("Registering user: " + user.getUsername());
        if (user.getUsername() == null || user.getPassword() == null) {
            throw new ResponseException(400, "bad request");
        }
        UserData existingUser = userDataAccess.getUser(user.getUsername());
        if (existingUser != null) {
            System.out.println("User already exists: " + user.getUsername());
            throw new ResponseException(403, "already taken");
        }
        String hashedPassword = AuthService.hashPassword(user.getPassword());
        userDataAccess.createUser(new UserData(user.getUsername(), hashedPassword, user.getEmail()));
        String authToken = AuthService.generateNewToken();
        AuthData authData = new AuthData(user.getUsername(), authToken);
        return authDataAccess.createAuth(authData);
    }

    public AuthData loginUser(LoginRequest loginRequest) throws ResponseException {
        System.out.println("Logging in user: " + loginRequest.getUsername());
        UserData user = userDataAccess.getUser(loginRequest.getUsername());
        if (user == null
                || !AuthService.verifyPassword(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseException(401, "unauthorized");
        }
        String authToken = AuthService.generateNewToken();
        AuthData authData = new AuthData(user.getUsername(), authToken);
        return authDataAccess.createAuth(authData);
    }

    public void logoutUser(String authToken) throws ResponseException {
        System.out.println("Logging out user: " + authToken);
        if (authDataAccess.getAuth(authToken) == null) {
            throw new ResponseException(401, "unauthorized");
        }
        authDataAccess.deleteAuth(authToken);
    }
}
