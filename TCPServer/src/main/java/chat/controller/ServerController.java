package chat.controller;

import chat.config.ServerConfig;
import chat.model.ChatServer;
import chat.model.ServerListener;
import chat.view.ServerView;
import javafx.application.Platform;

import java.io.IOException;

/**
 * Glue between the JavaFX view and the socket model. Implements {@link ServerListener} so the
 * model can report events; every event is marshalled onto the JavaFX thread before touching the view.
 */
public final class ServerController implements ServerListener {

    private final ServerView view;
    private final ChatServer server;

    public ServerController(ServerView view, ServerConfig config) {
        this.view = view;
        this.server = new ChatServer(config.bindAddress(), config.port(), this);
    }

    public void start() {
        try {
            server.start();
            view.setRunning(true, "Running on port " + server.getLocalPort());
        } catch (IOException e) {
            view.setRunning(false, "Failed to start");
            view.appendLog("ERROR: could not start server: " + e.getMessage());
        }
    }

    public void shutdown() {
        server.stop();
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> view.appendLog(message));
    }

    @Override
    public void onClientJoined(String username) {
        Platform.runLater(() -> view.addUser(username));
    }

    @Override
    public void onClientLeft(String username) {
        Platform.runLater(() -> view.removeUser(username));
    }
}
