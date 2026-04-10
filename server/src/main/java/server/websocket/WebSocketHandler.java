package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.AuthDataAccess;
import dataaccess.GameDataAccess;
import exception.ResponseException;
import io.javalin.websocket.*;
import model.AuthData;
import model.GameData;
import service.GameService;
import websocket.messages.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {
    private static final Gson GSON = new Gson();

    // gameID -> set of session IDs connected to that game
    private final Map<Integer, Set<String>> gameSessions = new ConcurrentHashMap<>();
    // sessionId -> gameID (for cleanup on close)
    private final Map<String, Integer> sessionGame = new ConcurrentHashMap<>();
    // sessionId -> WsContext (to be able to send messages later)
    private final Map<String, WsContext> sessionContexts = new ConcurrentHashMap<>();

    private final GameService gameService;
    private final AuthDataAccess authDataAccess;
    private final GameDataAccess gameDataAccess;

    public WebSocketHandler(GameService gameService, AuthDataAccess authDataAccess, GameDataAccess gameDataAccess) {
        this.gameService = gameService;
        this.authDataAccess = authDataAccess;
        this.gameDataAccess = gameDataAccess;
    }

    public void onConnect(WsConnectContext ctx) {
        sessionContexts.put(ctx.sessionId(), ctx);
    }

    public void onMessage(WsMessageContext ctx) {
        sessionContexts.put(ctx.sessionId(), ctx);
        var cmd = GSON.fromJson(ctx.message(), UserGameCommand.class);
        switch (cmd.getCommandType()) {
            case CONNECT   -> handleConnect(ctx, cmd);
            case MAKE_MOVE -> handleMakeMove(ctx, GSON.fromJson(ctx.message(), MakeMoveCommand.class));
            case LEAVE     -> handleLeave(ctx, cmd);
            case RESIGN    -> handleResign(ctx, cmd);
        }
    }

    public void onClose(WsCloseContext ctx) {
        String sid = ctx.sessionId();
        Integer gameID = sessionGame.remove(sid);
        if (gameID != null) {
            Set<String> sessions = gameSessions.get(gameID);
            if (sessions != null) {
                sessions.remove(sid);
            }
        }
        sessionContexts.remove(sid);
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = authDataAccess.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }
            GameData game = gameDataAccess.getGame(cmd.getGameID());
            if (game == null) {
                sendError(ctx, "Error: game not found");
                return;
            }
            gameSessions.computeIfAbsent(cmd.getGameID(), k -> ConcurrentHashMap.newKeySet()).add(ctx.sessionId());
            sessionGame.put(ctx.sessionId(), cmd.getGameID());

            sendToSession(ctx, new LoadGame(game));

            String notificationMsg;
            if (auth.getUsername().equals(game.getWhiteUsername())) {
                notificationMsg = auth.getUsername() + " joined as WHITE";
            } else if (auth.getUsername().equals(game.getBlackUsername())) {
                notificationMsg = auth.getUsername() + " joined as BLACK";
            } else {
                notificationMsg = auth.getUsername() + " joined as an observer";
            }
            broadcastExcept(cmd.getGameID(), ctx.sessionId(), new Notification(notificationMsg));
        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(WsMessageContext ctx, MakeMoveCommand cmd) {
        try {
            GameData game = gameService.makeMove(cmd.getAuthToken(), cmd.getGameID(), cmd.getMove());
            broadcastAll(cmd.getGameID(), new LoadGame(game));

            AuthData auth = authDataAccess.getAuth(cmd.getAuthToken());
            broadcastExcept(cmd.getGameID(), ctx.sessionId(),
                    new Notification(auth.getUsername() + " moved " + cmd.getMove()));

            ChessGame chessGame = game.getGame();
            ChessGame.TeamColor nextTurn = chessGame.getTeamTurn();
            String nextPlayer = nextTurn == ChessGame.TeamColor.WHITE
                    ? game.getWhiteUsername() : game.getBlackUsername();
            if (chessGame.isInCheckmate(nextTurn)) {
                broadcastAll(cmd.getGameID(), new Notification(nextPlayer + " is in checkmate! Game over."));
            } else if (chessGame.isInStalemate(nextTurn)) {
                broadcastAll(cmd.getGameID(), new Notification("Stalemate! The game is a draw."));
            } else if (chessGame.isInCheck(nextTurn)) {
                broadcastAll(cmd.getGameID(), new Notification(nextPlayer + " is in check!"));
            }
        } catch (ResponseException e) {
            sendError(ctx, e.getMessage());
        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(WsMessageContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = authDataAccess.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }
            gameService.leaveGame(cmd.getAuthToken(), cmd.getGameID());
            gameSessions.getOrDefault(cmd.getGameID(), Set.of()).remove(ctx.sessionId());
            sessionGame.remove(ctx.sessionId());
            broadcastAll(cmd.getGameID(), new Notification(auth.getUsername() + " left the game."));
        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void handleResign(WsMessageContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = authDataAccess.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }
            gameService.resignGame(cmd.getAuthToken(), cmd.getGameID());
            broadcastAll(cmd.getGameID(), new Notification(auth.getUsername() + " resigned. Game over."));
        } catch (ResponseException e) {
            sendError(ctx, e.getMessage());
        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void sendToSession(WsContext ctx, ServerMessage msg) {
        try {
            ctx.send(GSON.toJson(msg));
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    private void sendError(WsContext ctx, String errorMessage) {
        try {
            ctx.send(GSON.toJson(new ServerError(errorMessage)));
        } catch (Exception e) {
            System.err.println("Failed to send error: " + e.getMessage());
        }
    }

    private void broadcastAll(int gameID, ServerMessage msg) {
        broadcast(gameID, null, msg);
    }

    private void broadcastExcept(int gameID, String excludeSid, ServerMessage msg) {
        broadcast(gameID, excludeSid, msg);
    }

    private void broadcast(int gameID, String excludeSid, ServerMessage msg) {
        String json = GSON.toJson(msg);
        for (String sid : gameSessions.getOrDefault(gameID, Set.of())) {
            if (excludeSid != null && sid.equals(excludeSid)) {
                continue;
            }
            WsContext session = sessionContexts.get(sid);
            if (session != null) {
                try {
                    session.send(json);
                } catch (Exception e) {
                    System.err.println("Broadcast failed: " + e.getMessage());
                }
            }
        }
    }
}
