package ui;

import chess.ChessMove;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import jakarta.websocket.*;
import model.*;
import webSocketMessages.userCommands.*;

public class ServerFacade extends Endpoint {
    private static final Gson gson = new Gson();
    private final String serverUrl;
    private Session session;
    private final Consumer<String> onMessage;

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
        this.onMessage = null;
    }

    public ServerFacade(String serverUrl, Consumer<String> onMessage) {
        this.serverUrl = serverUrl;
        this.onMessage = onMessage;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public AuthData registerUser(UserData user) {
        var authData = fetch("POST", "/user", gson.toJson(user), null);
        return gson.fromJson(authData, AuthData.class);
    }

    public AuthData loginUser(LoginRequest loginRequest) {
        var authData = fetch("POST", "/session", gson.toJson(loginRequest), null);
        return gson.fromJson(authData, AuthData.class);
    }

    public void logoutUser(String authToken) {
        fetch("DELETE", "/session", "", authToken);
    }

    public GameData createGame(String authToken, GameData gameData) {
        var response = fetch("POST", "/game", gson.toJson(gameData), authToken);
        return gson.fromJson(response, GameData.class);
    }

    public ListGamesResponse listGames(String authToken) {
        var response = fetch("GET", "/game", "", authToken);
        return gson.fromJson(response, ListGamesResponse.class);
    }

    public GameData joinGame(String authToken, JoinGameRequest request) throws Exception {
        fetch("PUT", "/game", gson.toJson(request), authToken);
        var games = listGames(authToken);
        var gameData = games.getGames().stream()
                .filter(g -> g.getGameId() == request.getGameId())
                .findFirst()
                .orElseThrow();
        try {
            connect();
            send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, request.getGameId()));
        } catch (Exception e) {
            System.err.println("WebSocket connection failed: " + e.getMessage());
        }
        return gameData;
    }

    public void makeMove(String authToken, int gameID, ChessMove move) throws Exception {
        send(new MakeMoveCommand(authToken, gameID, move));
    }

    public void leaveGame(String authToken, int gameID) throws Exception {
        send(new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID));
    }

    public void resignGame(String authToken, int gameID) throws Exception {
        send(new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID));
    }

    private void connect() throws Exception {
        var uri = new URI(serverUrl.replace("http:", "ws:") + "/connect");
        System.out.println("Connecting to " + uri);
        var container = ContainerProvider.getWebSocketContainer();
        session = container.connectToServer(this, uri);
        session.addMessageHandler(
                new MessageHandler.Whole<String>() {
                    public void onMessage(String message) {
                        if (onMessage != null) onMessage.accept(message);
                    }
                });
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("Connected to websocket server.");
    }

    private void send(UserGameCommand command) throws Exception {
        var json = gson.toJson(command);
        System.out.println("Sending command: " + json);
        session.getBasicRemote().sendText(json);
    }

    private InputStreamReader fetch(String method, String path, String body, String authToken) {
        try {
            var http = sendRequest(method, serverUrl + path, body, authToken);
            var response = receiveResponse(http);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpURLConnection sendRequest(
            String method, String url, String body, String authToken)
            throws URISyntaxException, IOException {
        System.out.println("Sending " + method + " request to " + url);
        URI uri = new URI(url);
        HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
        if (authToken != null) http.setRequestProperty("Authorization", authToken);
        http.setRequestMethod(method);
        writeRequestBody(body, http);
        http.connect();
        return http;
    }

    private static void writeRequestBody(String body, HttpURLConnection http) throws IOException {
        if (!body.isEmpty()) {
            http.setDoOutput(true);
            try (var outputStream = http.getOutputStream()) {
                outputStream.write(body.getBytes());
            }
        }
    }

    private static InputStreamReader receiveResponse(HttpURLConnection http) throws IOException {
        InputStream responseBody = http.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(responseBody);
        return inputStreamReader;
    }
}
