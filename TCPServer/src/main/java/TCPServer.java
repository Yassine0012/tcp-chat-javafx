import chat.ServerApp;
import javafx.application.Application;

/**
 * Command line entry point: {@code java TCPServer}.
 * <p>
 * This class deliberately does NOT extend {@link Application}; that lets the shaded (fat) JAR start
 * without the "JavaFX runtime components are missing" error.
 */
public class TCPServer {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
