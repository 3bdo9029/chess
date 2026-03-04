package dataaccess;

import exception.ResponseException;
import java.util.Collection;
import model.GameData;

public interface GameDataAccess extends DataAccess {
    // Create a new game.
    public GameData createGame(GameData game) throws ResponseException;

    // Retrieve a specified game with the given game ID.
    public GameData getGame(int gameID) throws ResponseException;

    // Retrieve all games.
    public Collection<GameData> listGames() throws ResponseException;

    // Updates a chess game. It should replace the chess game string corresponding
    // to a given gameID. This is used when players join a game or when a move is
    // made.
    public GameData updateGame(int gameID, GameData game) throws ResponseException;
}
