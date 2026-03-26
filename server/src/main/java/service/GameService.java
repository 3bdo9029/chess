package service;

import chess.ChessGame.TeamColor;
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
}