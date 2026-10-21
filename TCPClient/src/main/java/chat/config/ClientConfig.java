package chat.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Where to find the server. Values come from {@code client.properties} (working directory first,
 * then the copy bundled in the JAR) and can be overridden by the command line:
 * <pre>java TCPClient &lt;ServerIPAddress&gt; &lt;PortNumber&gt;</pre>
 */
public record ClientConfig(String host, int port) {

    public static final String FILE_NAME = "client.properties";

    public static ClientConfig load(List<String> args) {
        Properties p = new Properties();
        p.setProperty("server.host", "localhost");
        p.setProperty("server.port", "3000");

        Path external = Path.of(FILE_NAME);
        try {
            if (Files.isRegularFile(external)) {
                try (InputStream in = Files.newInputStream(external)) {
                    p.load(in);
                }
            } else {
                try (InputStream in = ClientConfig.class.getResourceAsStream("/" + FILE_NAME)) {
                    if (in != null) p.load(in);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + FILE_NAME + ", using defaults: " + e.getMessage());
        }

        String host = p.getProperty("server.host").trim();
        String portText = p.getProperty("server.port").trim();

        if (args.size() == 2) {          // command-line arguments win over the file
            host = args.get(0);
            portText = args.get(1);
        } else if (!args.isEmpty()) {
            throw new IllegalArgumentException("Usage: java TCPClient <ServerIPAddress> <PortNumber>");
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be a number, got '" + portText + "'");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Server address must not be empty");
        }
        return new ClientConfig(host, port);
    }
}
