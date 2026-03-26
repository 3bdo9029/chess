package websocketmessages.servermessages;

public class ServerError extends ServerMessage {

    private final String errorMessage;

    public ServerError(String errorMessage) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
