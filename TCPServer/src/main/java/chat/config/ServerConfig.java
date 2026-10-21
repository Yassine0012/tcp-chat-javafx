package chat.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Network settings for the server, loaded at runtime from {@code server.properties}.
 * <p>
 * Lookup order: (1) a file named {@code server.properties} in the working directory
 * (edit it without recompiling), (2) the copy bundled inside the JAR, (3) built-in defaults.
 */
public record ServerConfig(String bindAddress, int port) {

    public static final String FILE_NAME = "server.properties";

    public static ServerConfig load() {
        Properties p = new Properties();
        p.setProperty("server.bindAddress", "0.0.0.0");
        p.setProperty("server.port", "3000");

        Path external = Path.of(FILE_NAME);
        try {
            if (Files.isRegularFile(external)) {
                try (InputStream in = Files.newInputStream(external)) {
                    p.load(in);
                }
            } else {
                try (InputStream in = ServerConfig.class.getResourceAsStream("/" + FILE_NAME)) {
                    if (in != null) p.load(in);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + FILE_NAME + ", using defaults: " + e.getMessage());
        }

        String host = p.getProperty("server.bindAddress").trim();
        int port;
        try {
            port = Integer.parseInt(p.getProperty("server.port").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("server.port must be a number in " + FILE_NAME);
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("server.port must be between 0 and 65535");
        }
        return new ServerConfig(host, port);
    }
}
