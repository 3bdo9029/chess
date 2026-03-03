package model;

import java.util.Collection;

public class ListGamesResponse {
    private Collection<GameData> games;

    public ListGamesResponse(Collection<GameData> games) {
        this.games = games;
    }

    public Collection<GameData> getGames() {
        return games;
    }
}
