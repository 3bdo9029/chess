package service;

import static org.junit.jupiter.api.Assertions.*;

import chess.ChessGame;
import chess.ChessGame.TeamColor;
import dataaccess.MemoryAuthDataAccess;
import dataaccess.MemoryGameDataAccess;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import model.JoinGameRequest;
import model.ListGamesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameServiceTest {
    private MemoryAuthDataAccess authDataAccess;
    private MemoryGameDataAccess gameDataAccess;
    private GameService gameService;
    private String validToken;

    @BeforeEach
    public void setUp() throws ResponseException {
        authDataAccess = new MemoryAuthDataAccess();
        gameDataAccess = new MemoryGameDataAccess();
        gameService = new GameService(authDataAccess, gameDataAccess);

        AuthData auth = authDataAccess.createAuth(new AuthData("alice", "valid-token"));
        validToken = auth.getAuthToken();
    }

    @Test
    public void listGamesSuccess() throws ResponseException {
        gameDataAccess.createGame(new GameData(0, null, null, "Game1", new ChessGame()));
        ListGamesResponse response = gameService.listGames(validToken);

        assertNotNull(response);
        assertEquals(1, response.getGames().size());
    }

    @Test
    public void listGamesEmptySuccess() throws ResponseException {
        ListGamesResponse response = gameService.listGames(validToken);
        assertNotNull(response);
        assertTrue(response.getGames().isEmpty());
    }

    @Test
    public void listGamesInvalidTokenFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.listGames("bad-token"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void createGameSuccess() throws ResponseException {
        GameData gameData = new GameData(0, null, null, "MyGame", null);
        GameData created = gameService.createGame(validToken, gameData);

        assertNotNull(created);
        assertEquals("MyGame", created.getGameName());
        assertNotNull(created.getGame());
    }

    @Test
    public void createGameInvalidTokenFails() {
        GameData gameData = new GameData(0, null, null, "MyGame", null);
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.createGame("bad-token", gameData));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void joinGameAsWhiteSuccess() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, created.getGameId());
        GameData updated = gameService.joinGame(validToken, req);

        assertEquals("alice", updated.getWhiteUsername());
    }

    @Test
    public void joinGameAsBlackSuccess() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(TeamColor.BLACK, created.getGameId());
        GameData updated = gameService.joinGame(validToken, req);

        assertEquals("alice", updated.getBlackUsername());
    }

    @Test
    public void joinGameInvalidTokenFails() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, created.getGameId());
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame("bad-token", req));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void joinGameInvalidGameIdFails() {
        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, 999);
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame(validToken, req));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void joinGameAlreadyTakenFails() throws ResponseException {
        AuthData auth2 = authDataAccess.createAuth(new AuthData("bob", "token2"));
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));

        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, created.getGameId());
        gameService.joinGame(validToken, req);

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame(auth2.getAuthToken(), req));
        assertEquals(403, ex.getStatusCode());
    }
}
