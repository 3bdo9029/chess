package webSocketMessages.serverMessages;

public class ServerMessage {
    public enum ServerMessageType { LOAD_GAME, ERROR, NOTIFICATION }

    private ServerMessageType serverMessageType;

    public ServerMessage(ServerMessageType serverMessageType) {
        this.serverMessageType = serverMessageType;
    }

    public ServerMessageType getServerMessageType() { return serverMessageType; }
}
