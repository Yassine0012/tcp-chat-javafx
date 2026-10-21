package chat;

import chat.config.ClientConfig;
import chat.controller.ClientController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** JavaFX entry point of the client (launched from {@code TCPClient.main}). */
public class ClientApp extends Application {

    private ClientController controller;

    @Override
    public void start(Stage stage) {
        ClientConfig config;
        try {
            config = ClientConfig.load(getParameters().getRaw());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            Platform.exit();
            return;
        }

        Scene scene = new Scene(new StackPane(), 760, 560);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        controller = new ClientController(config, scene);

        stage.setTitle("TCP Chat Client - " + config.host() + ":" + config.port());
        stage.setMinWidth(520);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) controller.shutdown();
    }
}
