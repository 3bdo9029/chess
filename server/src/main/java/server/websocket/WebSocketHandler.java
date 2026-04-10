package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.AuthDataAccess;
import dataaccess.GameDataAccess;
import exception.ResponseException;
import io.javalin.websocket.*;
import jakarta.websocket.Session;
import model.AuthData;
import model.GameData;
import service.GameService;
import websocketmessages.servermessages.*;
import websocketmessages.usercommands.MakeMoveCommand;
import websocketmessages.usercommands.UserGameCommand;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

}