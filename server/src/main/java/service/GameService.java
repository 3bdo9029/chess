ackage service;

import chess.ChessGame.TeamColor;
import dataAccess.*;
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
}