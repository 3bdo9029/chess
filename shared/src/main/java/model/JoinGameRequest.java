package model;

import chess.ChessGame.TeamColor;

public class JoinGameRequest {
    private TeamColor playerColor;
    private int gameID;

    public JoinGameRequest(TeamColor playerColor, int gameID) {
        this.playerColor = playerColor;
        this.gameID = gameID;
    }

    public TeamColor getPlayerColor() {
        return playerColor;
    }

    public int getGameId() {
        return gameID;
    }
}
