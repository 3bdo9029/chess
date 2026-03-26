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

    public void run() {
        System.out.println("Welcome to the Chess REPL!");
        System.out.println("Listening at " + this.serverFacade.getServerUrl());
        System.out.println("Type 'help' for a list of commands.");
        while (true) {
            if (gameData != null) printGameData();
            System.out.print("> ");
            var command = scanner.nextLine();
            switch (command) {
                case "register":
                    register();
                    break;
                case "login":
                    login();
                    break;
                case "logout":
                    logout();
                    break;
                case "games":
                    listGames();
                    break;
                case "new":
                    createGame();
                    break;
                case "join":
                    joinGame();
                    break;
                case "watch":
                    joinObserver();
                    break;
                case "redraw":
                    break;
                case "move":
                    makeMove();
                    break;
                case "moves":
                    highlightLegalMoves();
                    break;
                case "leave":
                    leaveGame();
                    break;
                case "resign":
                    resignGame();
                    break;
                case "quit":
                    quit();
                    break;
                case "help":
                    help();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
    }

    private void help() {
        System.out.println("Available commands:");
        if (authData == null) {
            System.out.println("  register - Create a new account");
            System.out.println("  login - Log in with an existing account");
        } else if (gameData == null) {
            System.out.println("  logout - Log out of " + authData.getUsername());
            System.out.println("  games - List all games");
            System.out.println("  new - Start a new game");
            System.out.println("  join - Join a game");
            System.out.println("  watch - Join a game as an observer");
        } else {
            System.out.println("  redraw - Redraw the board");
            System.out.println("  move - Make a move");
            System.out.println("  moves - Highlight legal moves");
            System.out.println("  leave - Leave the game");
            System.out.println("  resign - Forfeit the game");
        }
        System.out.println("  quit - Exit the program");
        System.out.println("  help - Show this help message");
    }

    private void quit() {
        System.out.println("Goodbye!");
        System.exit(0);
    }

    private void register() {
        System.out.print("Email: ");
        var email = scanner.nextLine();
        System.out.print("Username: ");
        var username = scanner.nextLine();
        System.out.print("Password: ");
        var password = new String(System.console().readPassword());
        var user = new UserData(username, password, email);
        try {
            authData = serverFacade.registerUser(user);
            System.out.println("Registered as " + authData.getUsername());
        } catch (Exception e) {
            System.out.println("Registration failed. Please try again.");
            System.out.println("Detail: " + e.getMessage());
        }
    }

    private void login() {
        System.out.print("Enter your username: ");
        var username = System.console().readLine();
        System.out.print("Enter your password: ");
        var password = new String(System.console().readPassword());
        System.out.printf("Logging in as %s...\n", username);
        try {
            authData = serverFacade.loginUser(new LoginRequest(username, password));
            System.out.printf("Logged in as %s.\n", authData.getUsername());
        } catch (Exception e) {
            System.out.println("Login failed. Please try again.");
            System.out.println("Detail: " + e.getMessage());
        }
    }

    private void logout() {
        System.out.printf("Logging out %s...\n", authData.getUsername());
        serverFacade.logoutUser(authData.getAuthToken());
        System.out.printf("Logged out %s.\n", authData.getUsername());
        authData = null;
    }
}