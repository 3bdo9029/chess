package server;

import com.google.gson.Gson;
import dataAccess.*;
import exception.ResponseException;
import model.*;
import service.*;
import spark.Request;
import spark.Response;
import spark.Spark;

public class Server {
    private static final Gson gson = new Gson();

    private final UserDataAccess userDataAccess;
    private final AuthDataAccess authDataAccess;
    private final GameDataAccess gameDataAccess;

    private final UserService userService;
    private final GameService gameService;
    private final DataService dataService;

    public Server() {
        try {
            // If you already have in-memory DAOs, use those instead for Phase 3.
            // Example:
            // userDataAccess = new MemoryUserDataAccess();
            // authDataAccess = new MemoryAuthDataAccess();
            // gameDataAccess = new MemoryGameDataAccess();

            userDataAccess = new SQLUserDataAccess();
            authDataAccess = new SQLAuthDataAccess();
            gameDataAccess = new SQLGameDataAccess();

            userService = new UserService(userDataAccess, authDataAccess);
            gameService = new GameService(authDataAccess, gameDataAccess);
            dataService = new DataService(userDataAccess, authDataAccess, gameDataAccess);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize server", ex);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        Spark.delete("/db", this::clearDatabase);
        Spark.post("/user", this::register);
        Spark.post("/session", this::login);
        Spark.delete("/session", this::logout);
        Spark.get("/game", this::listGames);
        Spark.post("/game", this::createGame);
        Spark.put("/game", this::joinGame);

        Spark.awaitInitialization();
        return Spark.port();
    }

    private String clearDatabase(Request request, Response response) {
        response.type("application/json");
        try {
            dataService.clearData();
            response.status(200);
            return "{}";
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String register(Request request, Response response) {
        response.type("application/json");
        try {
            UserData user = gson.fromJson(request.body(), UserData.class);
            response.status(200);
            return gson.toJson(userService.registerUser(user));
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String login(Request request, Response response) {
        response.type("application/json");
        try {
            LoginRequest loginRequest = gson.fromJson(request.body(), LoginRequest.class);
            response.status(200);
            return gson.toJson(userService.loginUser(loginRequest));
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String logout(Request request, Response response) {
        response.type("application/json");
        try {
            String authToken = request.headers("authorization");
            userService.logoutUser(authToken);
            response.status(200);
            return "{}";
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String listGames(Request request, Response response) {
        response.type("application/json");
        try {
            String authToken = request.headers("authorization");
            response.status(200);
            return gson.toJson(gameService.listGames(authToken));
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String createGame(Request request, Response response) {
        response.type("application/json");
        try {
            String authToken = request.headers("authorization");

            // If you made a CreateGameRequest class, use that instead.
            GameData gameData = gson.fromJson(request.body(), GameData.class);

            response.status(200);
            return gson.toJson(gameService.createGame(authToken, gameData));
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private String joinGame(Request request, Response response) {
        response.type("application/json");
        try {
            String authToken = request.headers("authorization");
            JoinGameRequest joinGameRequest = gson.fromJson(request.body(), JoinGameRequest.class);

            gameService.joinGame(authToken, joinGameRequest);

            response.status(200);
            return "{}";
        } catch (ResponseException e) {
            response.status(e.getStatusCode());
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
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