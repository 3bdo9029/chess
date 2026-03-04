package service;

import static org.junit.jupiter.api.Assertions.*;

import chess.ChessGame;
import dataaccess.MemoryAuthDataAccess;
import dataaccess.MemoryGameDataAccess;
import dataaccess.MemoryUserDataAccess;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataServiceTest {
    private MemoryUserDataAccess userDataAccess;
    private MemoryAuthDataAccess authDataAccess;
    private MemoryGameDataAccess gameDataAccess;
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        userDataAccess = new MemoryUserDataAccess();
        authDataAccess = new MemoryAuthDataAccess();
        gameDataAccess = new MemoryGameDataAccess();
        dataService = new DataService(userDataAccess, authDataAccess, gameDataAccess);
    }

    @Test
    public void clearDataSuccess() throws ResponseException {
        userDataAccess.createUser(new UserData("alice", "pass", "alice@example.com"));
        authDataAccess.createAuth(new AuthData("alice", "token1"));
        gameDataAccess.createGame(new GameData(0, null, null, "Game1", new ChessGame()));

        dataService.clearData();

        assertNull(userDataAccess.getUser("alice"));
        assertNull(authDataAccess.getAuth("token1"));
        assertTrue(gameDataAccess.listGames().isEmpty());
    }

    @Test
    public void clearEmptyDataSuccess() throws ResponseException {
        assertDoesNotThrow(() -> dataService.clearData());
        assertNull(userDataAccess.getUser("nobody"));
        assertTrue(gameDataAccess.listGames().isEmpty());
    }
}
