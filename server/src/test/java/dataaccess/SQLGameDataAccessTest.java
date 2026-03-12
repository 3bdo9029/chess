package dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import chess.ChessGame;
import exception.ResponseException;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SQLGameDataAccessTest {
    private SQLUserDataAccess userDao;
    private SQLGameDataAccess gameDao;

    @BeforeEach
    public void setUp() throws ResponseException {
        userDao = new SQLUserDataAccess();
        SQLAuthDataAccess authDao = new SQLAuthDataAccess();
        gameDao = new SQLGameDataAccess();
        authDao.clear();
        gameDao.clear();
        userDao.clear();
        userDao.createUser(new UserData("alice", "pass", "alice@example.com"));
        userDao.createUser(new UserData("bob", "pass", "bob@example.com"));
    }

    @Test
    public void clearSuccess() throws ResponseException {
        gameDao.createGame(new GameData(0, null, null, "MyGame", new ChessGame()));
        gameDao.clear();
        assertTrue(gameDao.listGames().isEmpty());
    }

    @Test
    public void clearEmptyTableSuccess() {
        assertDoesNotThrow(() -> gameDao.clear());
    }

    @Test
    public void createGameSuccess() throws ResponseException {
        GameData created = gameDao.createGame(new GameData(0, null, null, "Chess1", new ChessGame()));
        assertNotNull(created);
        assertTrue(created.getGameId() > 0);
    }

    @Test
    public void createGameNullNameThrows() {
        assertThrows(ResponseException.class,
                () -> gameDao.createGame(new GameData(0, null, null, null, new ChessGame())));
    }

    @Test
    public void updateGameSuccess() throws ResponseException {
        GameData created = gameDao.createGame(new GameData(0, null, null, "OldName", new ChessGame()));
        GameData updated = gameDao.updateGame(created.getGameId(),
                new GameData(created.getGameId(), "alice", null, "NewName", new ChessGame()));
        assertEquals("NewName", updated.getGameName());
        assertEquals("alice", updated.getWhiteUsername());
    }

    @Test
    public void updateNonexistentGameLeavesNoRow() throws ResponseException {
        gameDao.updateGame(99999, new GameData(99999, null, null, "Ghost", new ChessGame()));
        assertNull(gameDao.getGame(99999));
    }

    @Test
    public void getGameSuccess() throws ResponseException {
        GameData created = gameDao.createGame(new GameData(0, null, null, "FindMe", new ChessGame()));
        GameData found = gameDao.getGame(created.getGameId());
        assertNotNull(found);
        assertEquals("FindMe", found.getGameName());
    }

    @Test
    public void getGameNotFoundReturnsNull() throws ResponseException {
        assertNull(gameDao.getGame(99999));
    }

    @Test
    public void listGamesSuccess() throws ResponseException {
        gameDao.createGame(new GameData(0, null, null, "Game1", new ChessGame()));
        gameDao.createGame(new GameData(0, null, null, "Game2", new ChessGame()));
        assertEquals(2, gameDao.listGames().size());
    }

    @Test
    public void listGamesEmptySuccess() throws ResponseException {
        assertTrue(gameDao.listGames().isEmpty());
    }
}
