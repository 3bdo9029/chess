package websocketmessages.servermessages;

import model.GameData;

public class LoadGame extends ServerMessage {

    private final GameData gameData;

    public LoadGame(GameData gameData) {
        super(ServerMessageType.LOAD_GAME);
        this.gameData = gameData;
    }

    public GameData getGameData() {
        return gameData;
    }
}
