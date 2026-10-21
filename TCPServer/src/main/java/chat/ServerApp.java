package chat;

import chat.config.ServerConfig;
import chat.controller.ServerController;
import chat.view.ServerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX entry point of the server (launched from {@code TCPServer.main}). */
public class ServerApp extends Application {

    private ServerController controller;

    @Override
    public void start(Stage stage) {
        ServerConfig config = ServerConfig.load();
        ServerView view = new ServerView();
        controller = new ServerController(view, config);

        Scene scene = new Scene(view.getRoot(), 820, 520);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCP Chat Server");
        stage.setScene(scene);
        stage.show();

        controller.start();
    }

    @Override
    public void stop() {
        if (controller != null) controller.shutdown();
    }
}
