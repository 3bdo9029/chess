package ui;

import chess.*;
import com.google.gson.Gson;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import model.*;
import webSocketMessages.serverMessages.*;

public class Repl {
    private static final Gson gson = new Gson();
    private ServerFacade serverFacade;
    private AuthData authData;
    private GameData gameData;
    private Scanner scanner;

    public Repl(String serverUrl) {
        this.serverFacade = new ServerFacade(serverUrl, this::onMessage);
        this.scanner = new Scanner(System.in);
    }

    public void onMessage(String message) {
        System.out.println("");
        var serverMessage = gson.fromJson(message, ServerMessage.class);
        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME:
            {
                var load = gson.fromJson(message, LoadGame.class);
                gameData = load.getGameData();
                printGameData();
                break;
            }
            case ERROR:
            {
                var error = gson.fromJson(message, ServerError.class);
                System.err.println(error.getErrorMessage());
                break;
            }
            case NOTIFICATION:
            {
                var notification = gson.fromJson(message, Notification.class);
                System.out.println(notification.getMessage());
                break;
            }
        }
        System.out.print("> ");
    }

}