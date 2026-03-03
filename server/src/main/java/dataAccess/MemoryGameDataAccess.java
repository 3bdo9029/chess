package dataAccess;

import exception.ResponseException;
import java.util.ArrayList;
import java.util.Collection;
import model.GameData;

public class MemoryGameDataAccess implements GameDataAccess {
    private ArrayList<GameData> games = new ArrayList<GameData>();

    // Get the game index from the game ID.
    private static int getGameIndexFromID(int gameID) {
        return gameID - 1;
    }

    // Clears all games.
    public void clear() {
        games.clear();
    }

    // Create a new game.
    public GameData createGame(GameData game) {
        games.add(game);
        game.setGameId(games.size());
        return game;
    }

    // Retrieve a specified game with the given game ID.
    public GameData getGame(int gameID) {
        var index = getGameIndexFromID(gameID);
        if (index < 0 || index > games.size() - 1) {
            return null;
        }
        return games.get(getGameIndexFromID(gameID));
    }

    // Retrieve all games.
    public Collection<GameData> listGames() {
        return games;
    }

    // Updates a chess game. It should replace the chess game string corresponding
    // to a given gameID. This is used when players join a game or when a move is
    // made.
    public GameData updateGame(int gameID, GameData game) throws ResponseException {
        var index = getGameIndexFromID(gameID);
        if (index < 0 || index > games.size() - 1) {
            throw new ResponseException(404, "game not found");
        }
        games.set(index, game);
        return game;
    }
}
