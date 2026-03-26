package ui;

import chess.ChessMove;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import javax.websocket.*;
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

}