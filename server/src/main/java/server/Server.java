package server;

import chess.ChessGame.TeamColor;
import com.google.gson.Gson;
import dataAccess.*;
import exception.ResponseException;
import java.util.HashMap;
import java.util.HashSet;
import model.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.*;
import spark.Request;
import spark.Response;
import spark.Spark;
import webSocketMessages.serverMessages.*;
import webSocketMessages.userCommands.*;

@WebSocket
public class Server {
    private static final Gson gson = new Gson();
    private UserDataAccess userDataAccess;
    private AuthDataAccess authDataAccess;
    private GameDataAccess gameDataAccess;
    private UserService userService;
    private GameService gameService;
    private DataService dataService;

    // Track all connected sessions by gameID.
    private HashMap<Integer, HashSet<Session>> sessions = new HashMap<Integer, HashSet<Session>>();

    public Server() {
        try {
            userDataAccess = new SQLUserDataAccess();
            authDataAccess = new SQLAuthDataAccess();
            gameDataAccess = new SQLGameDataAccess();
            userService = new UserService(userDataAccess, authDataAccess);
            gameService = new GameService(authDataAccess, gameDataAccess);
            dataService = new DataService(userDataAccess, authDataAccess, gameDataAccess);
        } catch (Throwable ex) {
            System.err.println("Failed to initialize server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        System.out.printf("Received message: %s%n", message);
        var command = gson.fromJson(message, UserGameCommand.class);
        try {
            var authData = authDataAccess.getAuth(command.getAuthString());
            switch (command.getCommandType()) {
                case JOIN_OBSERVER:
                    joinObserver(session, message, authData);
                    break;
                case JOIN_PLAYER:
                    joinPlayer(session, message, authData);
                    break;
                case MAKE_MOVE:
                    makeMove(session, message, authData);
                    break;
                case LEAVE:
                    leave(session, message, authData);
                    break;
                case RESIGN:
                    resign(session, message, authData);
                    break;
            }
        } catch (Exception e) {
            var errorMessage =
                    String.format("Command %s failed: %s", command.getCommandType(), e.getMessage());
            System.err.println(errorMessage);
            System.err.println(e.getStackTrace());
            send(session, new ServerError(errorMessage));
        }
    }

    private void joinObserver(Session session, String message, AuthData authData) throws Exception {
        var join = gson.fromJson(message, JoinObserver.class);
        var gameData = gameDataAccess.getGame(join.getGameId());
        var notification = authData.getUsername() + " is now observing your game.";
        addSession(gameData.getGameId(), session);
        send(session, new LoadGame(gameData));
        sendToOthers(session, gameData.getGameId(), new Notification(notification));
    }

    private void joinPlayer(Session session, String message, AuthData authData) throws Exception {
        var join = gson.fromJson(message, JoinPlayer.class);
        var gameData = gameDataAccess.getGame(join.getGameId());

        // Check that the game aligns with the join request.
        if (join.getPlayerColor() == TeamColor.WHITE) {
            if (!authData.getUsername().equals(gameData.getWhiteUsername())) {
                throw new Exception("You are not the white player in this game.");
            }
        } else if (join.getPlayerColor() == TeamColor.BLACK) {
            if (!authData.getUsername().equals(gameData.getBlackUsername())) {
                throw new Exception("You are not the black player in this game.");
            }
        }

        var notification = "";
        if (join.getPlayerColor() == TeamColor.WHITE) {
            notification += gameData.getWhiteUsername();
            notification += " has joined the game playing white!";
        } else {
            notification += gameData.getBlackUsername();
            notification += " has joined the game playing black!";
        }

        addSession(gameData.getGameId(), session);
        send(session, new LoadGame(gameData));
        sendToOthers(session, gameData.getGameId(), new Notification(notification));
    }

    private void makeMove(Session session, String message, AuthData authData) throws Exception {
        var move = gson.fromJson(message, MakeMove.class);
        var gameData = gameDataAccess.getGame(move.getGameId());
        var game = gameData.getGame();

        var notification =
                String.format(
                        "%s moved their %s from %s to %s.",
                        authData.getUsername(),
                        game.getBoard().getPiece(move.getMove().getStartPosition()),
                        move.getMove().getStartPosition(),
                        move.getMove().getEndPosition());

        // The `makeMove()` method only checks that the piece being moved is
        // the color of the current player. It does not check that the
        // request is being made by the current player.
        if (game.getTeamTurn() == TeamColor.WHITE) {
            if (!authData.getUsername().equals(gameData.getWhiteUsername())) {
                throw new Exception("It is not your turn.");
            }
        } else {
            if (!authData.getUsername().equals(gameData.getBlackUsername())) {
                throw new Exception("It is not your turn.");
            }
        }

        game.makeMove(move.getMove());
        gameDataAccess.updateGame(gameData.getGameId(), gameData);
        sendToAll(move.getGameId(), new LoadGame(gameData));
        sendToOthers(session, move.getGameId(), new Notification(notification));

        // If the game is over, send a notification to all players.
        for (var teamColor : TeamColor.values()) {
            if (game.isInCheckmate(teamColor)) {
                sendToAll(
                        move.getGameId(),
                        new Notification("The game is in checkmate! " + teamColor + " has been defeated."));
            } else if (game.isInCheck(teamColor)) {
                sendToAll(
                        move.getGameId(),
                        new Notification("The game is in check! " + teamColor + " must save themselves!"));
            } else if (game.isInStalemate(teamColor)) {
                sendToAll(move.getGameId(), new Notification("The game is a stalemate!"));
            }
        }
    }

    private void leave(Session session, String message, AuthData authData) throws Exception {
        var leave = gson.fromJson(message, Leave.class);
        var gameData = gameDataAccess.getGame(leave.getGameId());
        if (gameData.getBlackUsername() == authData.getUsername()) {
            gameData.setBlackUsername(null);
        } else if (gameData.getWhiteUsername() == authData.getUsername()) {
            gameData.setWhiteUsername(null);
        }
        var notification = authData.getUsername() + " has left the game.";
        gameDataAccess.updateGame(gameData.getGameId(), gameData);
        removeSession(leave.getGameId(), session);
        sendToOthers(session, leave.getGameId(), new Notification(notification));
    }

    private void resign(Session session, String message, AuthData authData) throws Exception {
        var resign = gson.fromJson(message, Resign.class);
        var gameData = gameDataAccess.getGame(resign.getGameId());

        // Only allow players to resign. Observers can't resign.
        TeamColor resigned;
        if (authData.getUsername().equals(gameData.getWhiteUsername())) {
            resigned = TeamColor.WHITE;
        } else if (authData.getUsername().equals(gameData.getBlackUsername())) {
            resigned = TeamColor.BLACK;
        } else {
            throw new Exception("You are not a player in this game.");
        }

        // Only allow players to resign if the game is in progress.
        if (gameData.getGame().getResigned() != null) {
            throw new Exception("The game has already ended.");
        }

        gameData.getGame().setResigned(resigned);
        gameDataAccess.updateGame(gameData.getGameId(), gameData);
        var notification = authData.getUsername() + " has forfeited the game.";
        sendToAll(resign.getGameId(), new Notification(notification));
    }

    private void addSession(int gameID, Session session) {
        if (!sessions.containsKey(gameID)) sessions.put(gameID, new HashSet<Session>());
        sessions.get(gameID).add(session);
    }

    private void removeSession(int gameID, Session session) {
        if (sessions.containsKey(gameID)) sessions.get(gameID).remove(session);
    }

    private void send(Session session, ServerMessage message) throws Exception {
        session.getRemote().sendString(gson.toJson(message));
    }

    private void sendToAll(int gameID, ServerMessage message) throws Exception {
        sendToSessions(gameID, message, null);
    }

    private void sendToOthers(Session skip, int gameID, ServerMessage message) throws Exception {
        sendToSessions(gameID, message, skip);
    }

    private void sendToSessions(int gameID, ServerMessage message, Session skip) throws Exception {
        sessions
                .get(gameID)
                .forEach(
                        (session) -> {
                            if (skip != null && session.equals(skip)) {
                                System.out.println("Skipping current session...");
                            } else {
                                try {
                                    send(session, message);
                                } catch (Exception e) {
                                    System.err.println("Failed to send message to session: " + e.getMessage());
                                    System.err.println(e.getStackTrace());
                                }
                            }
                        });
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register endpoints. I have so few that I can put them all here. If this
        // becomes unwieldy, I'll move them to their own classes.
        // https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md#endpoint-specifications

        // Opens a websocket connection.
        Spark.webSocket("/connect", Server.class);

        // Clears the database. Removes all users, games, and authTokens.
        Spark.delete("/db", (request, response) -> clearDatabase(request, response));

        // Register a new user.
        Spark.post("/user", (request, response) -> register(request, response));

        // Logs in an existing user (returns a new authToken).
        Spark.post("/session", (request, response) -> login(request, response));

        // Logs out the user represented by the authToken.
        Spark.delete("/session", (request, response) -> logout(request, response));

        // Gives a list of all games.
        Spark.get("/game", (request, response) -> listGames(request, response));

        // Creates a new game.
        Spark.post("/game", (request, response) -> createGame(request, response));

        // Join a game. Verifies that the specified game exists, and, if a color is
        // specified, adds the caller as the requested color to the game. If no
        // color is specified the user is joined as an observer. This request is
        // idempotent.
        Spark.put("/game", (request, response) -> updateGame(request, response));

        Spark.awaitInitialization();
        return Spark.port();
    }

    private String clearDatabase(Request request, Response response) {
        try {
            dataService.clearData();
            return "";
        } catch (ResponseException e) {
            System.err.println("Error clearing data: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String register(Request request, Response response) {
        try {
            UserData user = gson.fromJson(request.body(), UserData.class);
            return gson.toJson(userService.registerUser(user));
        } catch (ResponseException e) {
            System.err.println("Error registering user: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String login(Request request, Response response) {
        try {
            LoginRequest loginRequest = gson.fromJson(request.body(), LoginRequest.class);
            return gson.toJson(userService.loginUser(loginRequest));
        } catch (ResponseException e) {
            System.err.println("Error logging in: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String logout(Request request, Response response) {
        try {
            String authToken = request.headers("Authorization");
            userService.logoutUser(authToken);
            return "";
        } catch (ResponseException e) {
            System.err.println("Error logging out: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String listGames(Request request, Response response) {
        try {
            String authToken = request.headers("Authorization");
            return gson.toJson(gameService.listGames(authToken));
        } catch (ResponseException e) {
            System.err.println("Error listing games: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String createGame(Request request, Response response) {
        try {
            String authToken = request.headers("Authorization");
            GameData gameData = gson.fromJson(request.body(), GameData.class);
            return gson.toJson(gameService.createGame(authToken, gameData));
        } catch (ResponseException e) {
            System.err.println("Error creating game: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private String updateGame(Request request, Response response) {
        try {
            String authToken = request.headers("Authorization");
            JoinGameRequest joinGameRequest = gson.fromJson(request.body(), JoinGameRequest.class);
            return gson.toJson(gameService.joinGame(authToken, joinGameRequest));
        } catch (ResponseException e) {
            System.err.println("Error joining game: " + e.getMessage());
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    public void clear() throws ResponseException {
        dataService.clearData();
    }
}
