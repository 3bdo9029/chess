package service;

import chess.ChessGame.TeamColor;
import chess.ChessMove;
import chess.InvalidMoveException;
import dataaccess.*;
import exception.ResponseException;
import model.*;
import server.*;
import chess.ChessGame;

public class GameService {
    private AuthDataAccess authDataAccess;
    private GameDataAccess gameDataAccess;

    public GameService(AuthDataAccess authDataAccess, GameDataAccess gameDataAccess) {
        this.authDataAccess = authDataAccess;
        this.gameDataAccess = gameDataAccess;
    }

    public ListGamesResponse listGames(String authToken) throws ResponseException {
        if (authDataAccess.getAuth(authToken) == null) {
            throw new ResponseException(401, "unauthorized");
        }
        return new ListGamesResponse(gameDataAccess.listGames());
    }

    public GameData createGame(String authToken, GameData gameData) throws ResponseException {
        if (authDataAccess.getAuth(authToken) == null) {
            throw new ResponseException(401, "unauthorized");
        }
        if (gameData.getGameName() == null) {
            throw new ResponseException(400, "bad request");
        }
        if (gameData.getGame() == null) {
            gameData.setGame(new ChessGame());
        }
        return gameDataAccess.createGame(gameData);
    }

    public GameData joinGame(String authToken, JoinGameRequest joinGameRequest)
            throws ResponseException {
        AuthData auth = authDataAccess.getAuth(authToken);
        if (auth == null) {
            throw new ResponseException(401, "unauthorized");
        }
        GameData game = gameDataAccess.getGame(joinGameRequest.getGameId());
        if (game == null) {
            throw new ResponseException(400, "bad request");
        }
        if (joinGameRequest.getPlayerColor() == TeamColor.WHITE) {
            if (auth.getUsername().equals(game.getWhiteUsername())) {
                // Already joined, nothing to do. The request is idempotent.
                return game;
            } else if (game.getWhiteUsername() != null) {
                throw new ResponseException(403, "already taken");
            }
            game.setWhiteUsername(auth.getUsername());
        } else if (joinGameRequest.getPlayerColor() == TeamColor.BLACK) {
            if (auth.getUsername().equals(game.getBlackUsername())) {
                // Already joined, nothing to do. The request is idempotent.
                return game;
            } else if (game.getBlackUsername() != null) {
                throw new ResponseException(403, "already taken");
            }
            game.setBlackUsername(auth.getUsername());
        } else {
            throw new ResponseException(400, "bad request");
        }
        return gameDataAccess.updateGame(game.getGameId(), game);
    }

    public GameData makeMove(String authToken, int gameID, ChessMove move) throws ResponseException {
        AuthData auth = authDataAccess.getAuth(authToken);
        if (auth == null) {
            throw new ResponseException(401, "Error: unauthorized");
        }
        GameData game = gameDataAccess.getGame(gameID);
        if (game == null) {
            throw new ResponseException(400, "Error: bad request");
        }
        if (game.getGame().isGameOver()) {
            throw new ResponseException(400, "Error: game is over");
        }
        TeamColor playerColor = getPlayerColor(auth.getUsername(), game);
        if (playerColor == null) {
            throw new ResponseException(403, "Error: observers cannot make moves");
        }
        if (game.getGame().getTeamTurn() != playerColor) {
            throw new ResponseException(400, "Error: not your turn");
        }
        try {
            game.getGame().makeMove(move);
        } catch (InvalidMoveException e) {
            throw new ResponseException(400, "Error: " + e.getMessage());
        }
        TeamColor nextTurn = game.getGame().getTeamTurn();
        if (game.getGame().isInCheckmate(nextTurn) || game.getGame().isInStalemate(nextTurn)) {
            game.getGame().setGameOver(true);
        }
        return gameDataAccess.updateGame(gameID, game);
    }

    public void leaveGame(String authToken, int gameID) throws ResponseException {
        AuthData auth = authDataAccess.getAuth(authToken);
        if (auth == null) {
            throw new ResponseException(401, "Error: unauthorized");
        }
        GameData game = gameDataAccess.getGame(gameID);
        if (game == null) {
            throw new ResponseException(400, "Error: bad request");
        }
        if (auth.getUsername().equals(game.getWhiteUsername())) {
            game.setWhiteUsername(null);
        } else if (auth.getUsername().equals(game.getBlackUsername())) {
            game.setBlackUsername(null);
        }
        gameDataAccess.updateGame(gameID, game);
    }

    public void resignGame(String authToken, int gameID) throws ResponseException {
        AuthData auth = authDataAccess.getAuth(authToken);
        if (auth == null) {
            throw new ResponseException(401, "Error: unauthorized");
        }
        GameData game = gameDataAccess.getGame(gameID);
        if (game == null) {
            throw new ResponseException(400, "Error: bad request");
        }
        TeamColor playerColor = getPlayerColor(auth.getUsername(), game);
        if (playerColor == null) {
            throw new ResponseException(403, "Error: observers cannot resign");
        }
        if (game.getGame().isGameOver()) {
            throw new ResponseException(400, "Error: game is already over");
        }
        game.getGame().setResigned(playerColor);
        gameDataAccess.updateGame(gameID, game);
    }

    private TeamColor getPlayerColor(String username, GameData game) {
        if (username.equals(game.getWhiteUsername())) {
            return TeamColor.WHITE;
        }
        if (username.equals(game.getBlackUsername())) {
            return TeamColor.BLACK;
        }
        return null;
    }
}