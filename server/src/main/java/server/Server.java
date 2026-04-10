package server;

import com.google.gson.Gson;
import dataaccess.*;
import exception.ResponseException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import model.*;
import server.websocket.WebSocketHandler;
import service.*;

public class Server {
    private static final Gson GSON = new Gson();

    private final Javalin javalin;

    private final UserDataAccess userDataAccess;
    private final AuthDataAccess authDataAccess;
    private final GameDataAccess gameDataAccess;

    private final UserService userService;
    private final GameService gameService;
    private final DataService dataService;

    public Server() {
        try {
            userDataAccess = new SQLUserDataAccess();
            authDataAccess = new SQLAuthDataAccess();
            gameDataAccess = new SQLGameDataAccess();

            userService = new UserService(userDataAccess, authDataAccess);
            gameService = new GameService(authDataAccess, gameDataAccess);
            dataService = new DataService(userDataAccess, authDataAccess, gameDataAccess);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize server", ex);
        }

        var wsHandler = new WebSocketHandler(gameService, authDataAccess, gameDataAccess);
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.ws("/ws", ws -> {
            ws.onConnect(wsHandler::onConnect);
            ws.onMessage(wsHandler::onMessage);
            ws.onClose(wsHandler::onClose);
        });

        javalin.delete("/db", this::clearDatabase);
        javalin.post("/user", this::register);
        javalin.post("/session", this::login);
        javalin.delete("/session", this::logout);
        javalin.get("/game", this::listGames);
        javalin.post("/game", this::createGame);
        javalin.put("/game", this::joinGame);
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    private void clearDatabase(Context ctx) {
        ctx.contentType("application/json");
        try {
            dataService.clearData();
            ctx.status(200);
            ctx.result("{}");
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void register(Context ctx) {
        ctx.contentType("application/json");
        try {
            UserData user = GSON.fromJson(ctx.body(), UserData.class);
            ctx.status(200);
            ctx.result(GSON.toJson(userService.registerUser(user)));
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void login(Context ctx) {
        ctx.contentType("application/json");
        try {
            LoginRequest loginRequest = GSON.fromJson(ctx.body(), LoginRequest.class);
            ctx.status(200);
            ctx.result(GSON.toJson(userService.loginUser(loginRequest)));
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void logout(Context ctx) {
        ctx.contentType("application/json");
        try {
            String authToken = ctx.header("authorization");
            userService.logoutUser(authToken);
            ctx.status(200);
            ctx.result("{}");
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void listGames(Context ctx) {
        ctx.contentType("application/json");
        try {
            String authToken = ctx.header("authorization");
            ctx.status(200);
            ctx.result(GSON.toJson(gameService.listGames(authToken)));
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void createGame(Context ctx) {
        ctx.contentType("application/json");
        try {
            String authToken = ctx.header("authorization");
            GameData gameData = GSON.fromJson(ctx.body(), GameData.class);

            ctx.status(200);
            ctx.result(GSON.toJson(gameService.createGame(authToken, gameData)));
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    private void joinGame(Context ctx) {
        ctx.contentType("application/json");
        try {
            String authToken = ctx.header("authorization");
            JoinGameRequest joinGameRequest = GSON.fromJson(ctx.body(), JoinGameRequest.class);

            gameService.joinGame(authToken, joinGameRequest);

            ctx.status(200);
            ctx.result("{}");
        } catch (ResponseException e) {
            ctx.status(e.getStatusCode());
            ctx.result(GSON.toJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            ctx.status(500);
            ctx.result(GSON.toJson(new ErrorResponse("Error: " + e.getMessage())));
        }
    }

    public void stop() {
        javalin.stop();
    }

    public void clear() throws ResponseException {
        dataService.clearData();
    }
}