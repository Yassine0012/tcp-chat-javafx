import chat.ClientApp;
import javafx.application.Application;

/**
 * Command line entry point: {@code java TCPClient <ServerIPAddress> <PortNumber>}
 * (for example {@code java TCPClient localhost 3000}).
 * <p>
 * This class deliberately does NOT extend {@link Application}; that lets the shaded (fat) JAR start
 * without the "JavaFX runtime components are missing" error.
 */
public class TCPClient {
    public static void main(String[] args) {
        if (args.length != 0 && args.length != 2) {
            System.err.println("Usage: java TCPClient <ServerIPAddress> <PortNumber>   (e.g. java TCPClient localhost 3000)");
            System.exit(1);
        }
        Application.launch(ClientApp.class, args);
    }
}
