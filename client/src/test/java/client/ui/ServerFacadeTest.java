package client.ui;

import chess.ChessGame;
import model.*;
import org.junit.jupiter.api.*;
import server.Server;
import ui.ServerFacade;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTest {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void startServer() {
        server = new Server();
        int port = server.run(0);
        facade = new ServerFacade("http://localhost:" + port);
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearDb() throws Exception {
        facade.clearDatabase();
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    public void registerUserSuccess() {
        var auth = facade.registerUser(new UserData("alice", "pw", "a@x.com"));
        assertNotNull(auth);
        assertEquals("alice", auth.getUsername());
        assertNotNull(auth.getAuthToken());
    }

    @Test
    public void registerUserDuplicateUsername() {
        facade.registerUser(new UserData("alice", "pw", "a@x.com"));
        assertThrows(RuntimeException.class,
                () -> facade.registerUser(new UserData("alice", "pw2", "b@x.com")));
    }

    // ── loginUser ─────────────────────────────────────────────────────────────

    @Test
    public void loginUserSuccess() {
        facade.registerUser(new UserData("bob", "secret", "b@x.com"));
        var auth = facade.loginUser(new LoginRequest("bob", "secret"));
        assertNotNull(auth);
        assertEquals("bob", auth.getUsername());
    }

    @Test
    public void loginUserWrongPassword() {
        facade.registerUser(new UserData("bob", "secret", "b@x.com"));
        assertThrows(RuntimeException.class,
                () -> facade.loginUser(new LoginRequest("bob", "wrong")));
    }

    // ── logoutUser ────────────────────────────────────────────────────────────

    @Test
    public void logoutUserSuccess() {
        var auth = facade.registerUser(new UserData("carol", "pw", "c@x.com"));
        assertDoesNotThrow(() -> facade.logoutUser(auth.getAuthToken()));
    }

    @Test
    public void logoutUserInvalidToken() {
        assertThrows(RuntimeException.class, () -> facade.logoutUser("not-a-real-token"));
    }

    // ── createGame ────────────────────────────────────────────────────────────

    @Test
    public void createGameSuccess() {
        var auth = facade.registerUser(new UserData("dave", "pw", "d@x.com"));
        var game = facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "TestGame", new ChessGame()));
        assertNotNull(game);
        assertTrue(game.getGameId() > 0);
    }

    @Test
    public void createGameNoAuth() {
        assertThrows(RuntimeException.class,
                () -> facade.createGame("bad-token",
                        new GameData(0, null, null, "TestGame", new ChessGame())));
    }

    // ── listGames ─────────────────────────────────────────────────────────────

    @Test
    public void listGamesSuccess() {
        var auth = facade.registerUser(new UserData("eve", "pw", "e@x.com"));
        facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "G1", new ChessGame()));
        facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "G2", new ChessGame()));
        var list = facade.listGames(auth.getAuthToken());
        assertNotNull(list);
        assertEquals(2, list.getGames().size());
    }

    @Test
    public void listGamesInvalidAuth() {
        assertThrows(RuntimeException.class, () -> facade.listGames("bad-token"));
    }

    // ── joinGame ──────────────────────────────────────────────────────────────

    @Test
    public void joinGameSuccess() throws Exception {
        var auth = facade.registerUser(new UserData("frank", "pw", "f@x.com"));
        var game = facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "JoinMe", new ChessGame()));
        var joined = facade.joinGame(auth.getAuthToken(),
                new JoinGameRequest(ChessGame.TeamColor.WHITE, game.getGameId()));
        assertNotNull(joined);
        assertEquals(game.getGameId(), joined.getGameId());
    }

    @Test
    public void joinGameInvalidId() {
        var auth = facade.registerUser(new UserData("grace", "pw", "g@x.com"));
        assertThrows(Exception.class,
                () -> facade.joinGame(auth.getAuthToken(),
                        new JoinGameRequest(ChessGame.TeamColor.WHITE, 99999)));
    }

    // ── joinGame as observer ──────────────────────────────────────────────────

    @Test
    public void joinGameAsObserverSuccess() throws Exception {
        var auth = facade.registerUser(new UserData("henry", "pw", "h@x.com"));
        var game = facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "Watch", new ChessGame()));
        var joined = facade.joinGame(auth.getAuthToken(),
                new JoinGameRequest(null, game.getGameId()));
        assertNotNull(joined);
    }

    @Test
    public void joinGameColorTaken() throws Exception {
        var auth1 = facade.registerUser(new UserData("ivan", "pw", "i@x.com"));
        var auth2 = facade.registerUser(new UserData("judy", "pw", "j@x.com"));
        var game = facade.createGame(auth1.getAuthToken(),
                new GameData(0, null, null, "Clash", new ChessGame()));
        facade.joinGame(auth1.getAuthToken(),
                new JoinGameRequest(ChessGame.TeamColor.WHITE, game.getGameId()));
        assertThrows(Exception.class,
                () -> facade.joinGame(auth2.getAuthToken(),
                        new JoinGameRequest(ChessGame.TeamColor.WHITE, game.getGameId())));
    }

    // ── createGame name variations ────────────────────────────────────────────

    @Test
    public void createMultipleGamesDistinctIds() {
        var auth = facade.registerUser(new UserData("karl", "pw", "k@x.com"));
        var g1 = facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "A", new ChessGame()));
        var g2 = facade.createGame(auth.getAuthToken(),
                new GameData(0, null, null, "B", new ChessGame()));
        assertNotEquals(g1.getGameId(), g2.getGameId());
    }

    // ── listGames empty ───────────────────────────────────────────────────────

    @Test
    public void listGamesEmpty() {
        var auth = facade.registerUser(new UserData("lisa", "pw", "l@x.com"));
        var list = facade.listGames(auth.getAuthToken());
        assertNotNull(list);
        assertEquals(0, list.getGames().size());
    }
}
