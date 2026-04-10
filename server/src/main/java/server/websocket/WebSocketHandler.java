package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.AuthDataAccess;
import dataaccess.GameDataAccess;
import exception.ResponseException;
import io.javalin.websocket.*;
import jakarta.websocket.Session;
import model.AuthData;
import model.GameData;
import service.GameService;
import websocketmessages.servermessages.*;
import websocketmessages.usercommands.MakeMoveCommand;
import websocketmessages.usercommands.UserGameCommand;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {
    private static final Gson GSON = new Gson();

    // gameID -> set of sessions connected to that game
    private final Map<Integer, Set<Session>> gameSessions = new ConcurrentHashMap<>();
    // session -> gameID (for cleanup on close)
    private final Map<Session, Integer> sessionGame = new ConcurrentHashMap<>();

    private final GameService gameService;
    private final AuthDataAccess authDataAccess;
    private final GameDataAccess gameDataAccess;

    public WebSocketHandler(GameService gameService, AuthDataAccess authDataAccess, GameDataAccess gameDataAccess) {
        this.gameService = gameService;
        this.authDataAccess = authDataAccess;
        this.gameDataAccess = gameDataAccess;
    }

    public void onConnect(WsConnectContext ctx) {}

    public void onMessage(WsMessageContext ctx) {
        var cmd = GSON.fromJson(ctx.message(), UserGameCommand.class);
        switch (cmd.getCommandType()) {
            case CONNECT   -> handleConnect(ctx, cmd);
            case MAKE_MOVE -> handleMakeMove(ctx, GSON.fromJson(ctx.message(), MakeMoveCommand.class));
            case LEAVE     -> handleLeave(ctx, cmd);
            case RESIGN    -> handleResign(ctx, cmd);
        }
    }

    public void onClose(WsCloseContext ctx) {
        Integer gameID = sessionGame.remove(ctx.session());
        if (gameID != null) {
            Set<Session> sessions = gameSessions.get(gameID);
            if (sessions != null) {
                sessions.remove(ctx.session());
            }
        }
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand cmd) {
        try {
            AuthData auth = authDataAccess.getAuth(cmd.getAuthToken());
            if (auth == null) {
                sendError(ctx.session(), "Error: unauthorized");
                return;
            }
            GameData game = gameDataAccess.getGame(cmd.getGameID());
            if (game == null) {
                sendError(ctx.session(), "Error: game not found");
                return;
            }
            gameSessions.computeIfAbsent(cmd.getGameID(), k -> ConcurrentHashMap.newKeySet()).add(ctx.session());
            sessionGame.put(ctx.session(), cmd.getGameID());

            sendToSession(ctx.session(), new LoadGame(game));

            String notificationMsg;
            if (auth.getUsername().equals(game.getWhiteUsername())) {
                notificationMsg = auth.getUsername() + " joined as WHITE";
            } else if (auth.getUsername().equals(game.getBlackUsername())) {
                notificationMsg = auth.getUsername() + " joined as BLACK";
            } else {
                notificationMsg = auth.getUsername() + " joined as an observer";
            }
            broadcastExcept(cmd.getGameID(), ctx.session(), new Notification(notificationMsg));
        } catch (Exception e) {
            sendError(ctx.session(), "Error: " + e.getMessage());
        }
    }
}