package service;

import static org.junit.jupiter.api.Assertions.*;

import chess.ChessGame;
import chess.ChessGame.TeamColor;
import chess.ChessMove;
import chess.ChessPosition;
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
    private String bobToken;

    @BeforeEach
    public void setUp() throws ResponseException {
        authDataAccess = new MemoryAuthDataAccess();
        gameDataAccess = new MemoryGameDataAccess();
        gameService = new GameService(authDataAccess, gameDataAccess);

        AuthData auth = authDataAccess.createAuth(new AuthData("alice", "valid-token"));
        validToken = auth.getAuthToken();
        bobToken = authDataAccess.createAuth(new AuthData("bob", "bob-token")).getAuthToken();
    }

    /** Creates a game with alice as white and bob as black and returns its id. */
    private int createGameWithPlayers() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "Match", null));
        int gameId = created.getGameId();
        gameService.joinGame(validToken, new JoinGameRequest(TeamColor.WHITE, gameId));
        gameService.joinGame(bobToken, new JoinGameRequest(TeamColor.BLACK, gameId));
        return gameId;
    }

    private static ChessMove move(int fromRow, int fromCol, int toRow, int toCol) {
        return new ChessMove(new ChessPosition(fromRow, fromCol), new ChessPosition(toRow, toCol), null);
    }

    // ---------- listGames ----------

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

    // ---------- createGame ----------

    @Test
    public void createGameSuccess() throws ResponseException {
        GameData gameData = new GameData(0, null, null, "MyGame", null);
        GameData created = gameService.createGame(validToken, gameData);

        assertNotNull(created);
        assertEquals("MyGame", created.getGameName());
        assertNotNull(created.getGame());
    }

    @Test
    public void createGameKeepsProvidedChessGame() throws ResponseException {
        ChessGame chessGame = new ChessGame();
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "MyGame", chessGame));

        assertSame(chessGame, created.getGame());
    }

    @Test
    public void createGameInvalidTokenFails() {
        GameData gameData = new GameData(0, null, null, "MyGame", null);
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.createGame("bad-token", gameData));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void createGameNullNameFails() {
        GameData gameData = new GameData(0, null, null, null, null);
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.createGame(validToken, gameData));
        assertEquals(400, ex.getStatusCode());
    }

    // ---------- joinGame ----------

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
    public void joinGameSameWhiteTwiceIsIdempotent() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, created.getGameId());
        gameService.joinGame(validToken, req);
        GameData again = gameService.joinGame(validToken, req);

        assertEquals("alice", again.getWhiteUsername());
        assertNull(again.getBlackUsername());
    }

    @Test
    public void joinGameSameBlackTwiceIsIdempotent() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(TeamColor.BLACK, created.getGameId());
        gameService.joinGame(validToken, req);
        GameData again = gameService.joinGame(validToken, req);

        assertEquals("alice", again.getBlackUsername());
        assertNull(again.getWhiteUsername());
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
    public void joinGameNullColorFails() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));
        JoinGameRequest req = new JoinGameRequest(null, created.getGameId());
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame(validToken, req));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void joinGameAlreadyTakenFails() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));

        JoinGameRequest req = new JoinGameRequest(TeamColor.WHITE, created.getGameId());
        gameService.joinGame(validToken, req);

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame(bobToken, req));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    public void joinGameBlackAlreadyTakenFails() throws ResponseException {
        GameData created = gameService.createGame(validToken,
                new GameData(0, null, null, "TestGame", null));

        JoinGameRequest req = new JoinGameRequest(TeamColor.BLACK, created.getGameId());
        gameService.joinGame(validToken, req);

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.joinGame(bobToken, req));
        assertEquals(403, ex.getStatusCode());
    }

    // ---------- makeMove ----------

    @Test
    public void makeMoveSuccess() throws ResponseException {
        int gameId = createGameWithPlayers();
        GameData updated = gameService.makeMove(validToken, gameId, move(2, 5, 4, 5));

        assertEquals(TeamColor.BLACK, updated.getGame().getTeamTurn());
        assertNotNull(updated.getGame().getBoard().getPiece(new ChessPosition(4, 5)));
        assertNull(updated.getGame().getBoard().getPiece(new ChessPosition(2, 5)));
        assertFalse(updated.getGame().isGameOver());
    }

    @Test
    public void makeMoveCheckmateEndsGame() throws ResponseException {
        int gameId = createGameWithPlayers();
        // Fools mate: 1. f3 e5 2. g4 Qh4#
        gameService.makeMove(validToken, gameId, move(2, 6, 3, 6));
        gameService.makeMove(bobToken, gameId, move(7, 5, 5, 5));
        gameService.makeMove(validToken, gameId, move(2, 7, 4, 7));
        GameData updated = gameService.makeMove(bobToken, gameId, move(8, 4, 4, 8));

        assertTrue(updated.getGame().isInCheckmate(TeamColor.WHITE));
        assertTrue(updated.getGame().isGameOver());
    }

    @Test
    public void makeMoveInvalidTokenFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove("bad-token", gameId, move(2, 5, 4, 5)));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void makeMoveInvalidGameIdFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove(validToken, 999, move(2, 5, 4, 5)));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void makeMoveGameOverFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameDataAccess.getGame(gameId).getGame().setGameOver(true);

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove(validToken, gameId, move(2, 5, 4, 5)));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void makeMoveObserverFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        String observerToken =
                authDataAccess.createAuth(new AuthData("carol", "carol-token")).getAuthToken();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove(observerToken, gameId, move(2, 5, 4, 5)));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    public void makeMoveNotYourTurnFails() throws ResponseException {
        int gameId = createGameWithPlayers();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove(bobToken, gameId, move(7, 5, 5, 5)));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void makeMoveIllegalMoveFails() throws ResponseException {
        int gameId = createGameWithPlayers();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.makeMove(validToken, gameId, move(2, 5, 5, 5)));
        assertEquals(400, ex.getStatusCode());
        assertEquals(TeamColor.WHITE, gameDataAccess.getGame(gameId).getGame().getTeamTurn());
    }

    // ---------- leaveGame ----------

    @Test
    public void leaveGameAsWhiteSuccess() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameService.leaveGame(validToken, gameId);

        GameData game = gameDataAccess.getGame(gameId);
        assertNull(game.getWhiteUsername());
        assertEquals("bob", game.getBlackUsername());
    }

    @Test
    public void leaveGameAsBlackSuccess() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameService.leaveGame(bobToken, gameId);

        GameData game = gameDataAccess.getGame(gameId);
        assertEquals("alice", game.getWhiteUsername());
        assertNull(game.getBlackUsername());
    }

    @Test
    public void leaveGameAsObserverLeavesPlayersUnchanged() throws ResponseException {
        int gameId = createGameWithPlayers();
        String observerToken =
                authDataAccess.createAuth(new AuthData("carol", "carol-token")).getAuthToken();
        gameService.leaveGame(observerToken, gameId);

        GameData game = gameDataAccess.getGame(gameId);
        assertEquals("alice", game.getWhiteUsername());
        assertEquals("bob", game.getBlackUsername());
    }

    @Test
    public void leaveGameInvalidTokenFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.leaveGame("bad-token", gameId));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void leaveGameInvalidGameIdFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.leaveGame(validToken, 999));
        assertEquals(400, ex.getStatusCode());
    }

    // ---------- resignGame ----------

    @Test
    public void resignGameSuccess() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameService.resignGame(validToken, gameId);

        ChessGame game = gameDataAccess.getGame(gameId).getGame();
        assertEquals(TeamColor.WHITE, game.getResigned());
        assertTrue(game.isGameOver());
    }

    @Test
    public void resignGameAsBlackSuccess() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameService.resignGame(bobToken, gameId);

        assertEquals(TeamColor.BLACK, gameDataAccess.getGame(gameId).getGame().getResigned());
    }

    @Test
    public void resignGameInvalidTokenFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.resignGame("bad-token", gameId));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void resignGameInvalidGameIdFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.resignGame(validToken, 999));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void resignGameObserverFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        String observerToken =
                authDataAccess.createAuth(new AuthData("carol", "carol-token")).getAuthToken();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.resignGame(observerToken, gameId));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    public void resignGameAlreadyOverFails() throws ResponseException {
        int gameId = createGameWithPlayers();
        gameService.resignGame(validToken, gameId);

        ResponseException ex = assertThrows(ResponseException.class,
                () -> gameService.resignGame(bobToken, gameId));
        assertEquals(400, ex.getStatusCode());
    }
}
