package service;

import dataaccess.*;
import exception.ResponseException;

public class DataService {
    private UserDataAccess userDataAccess;
    private AuthDataAccess authDataAccess;
    private GameDataAccess gameDataAccess;

    public DataService(
            UserDataAccess userDataAccess, AuthDataAccess authDataAccess, GameDataAccess gameDataAccess) {
        this.userDataAccess = userDataAccess;
        this.authDataAccess = authDataAccess;
        this.gameDataAccess = gameDataAccess;
    }

    public void clearData() throws ResponseException {
        System.out.println("Clearing data...");
        authDataAccess.clear();
        gameDataAccess.clear();
        userDataAccess.clear();
    }
}
