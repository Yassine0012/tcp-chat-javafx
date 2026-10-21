package chat.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Pure-Java chat server (the "Model"). Accepts TCP connections and starts one
 * <b>virtual thread</b> per client. Contains no JavaFX code.
 *
 * <h3>Wire protocol (one UTF-8 text line per message, fields separated by '|')</h3>
 * <pre>
 * client -> server   JOIN|name        (empty name = read-only guest)
 *                    MSG|text
 *                    ALLUSERS
 *                    BYE
 * server -> client   OK|name|readOnly
 *                    ERR|reason
 *                    SYS|text
 *                    MSG|HH:mm:ss|sender|text
 *                    USERS|a,b,c
 * </pre>
 */
public class ChatServer {

    static final Pattern VALID_NAME = Pattern.compile("[\\w.-]{1,20}");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String bindAddress;
    private final int port;
    private final ServerListener listener;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger guestCounter = new AtomicInteger();

    private volatile ExecutorService executor;
    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(String bindAddress, int port, ServerListener listener) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.listener = listener;
    }

    /** Binds the socket and starts accepting clients on a virtual thread. */
    public synchronized void start() throws IOException {
        if (running) return;
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(InetAddress.getByName(bindAddress), port));
        serverSocket = ss;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        running = true;
        listener.onLog("Server Started on " + ss.getInetAddress().getHostAddress() + ":" + ss.getLocalPort());
        executor.submit(this::acceptLoop);
    }

    /** Closes the listening socket and every client connection. */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        for (ClientHandler h : clients) h.close();
        executor.shutdownNow();
        listener.onLog("Server Stopped");
    }

    public boolean isRunning() {
        return running;
    }

    /** Actual bound port (useful when configured with port 0). */
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    public List<String> userNames() {
        return clients.stream().map(ClientHandler::getUsername).toList();
    }

    // ---------------------------------------------------------------- internals

    private void acceptLoop() {
        while (running) {
            try {
                listener.onLog("Waiting for Client...");
                Socket socket = serverSocket.accept();
                listener.onLog("Connection from " + socket.getRemoteSocketAddress());
                executor.submit(new ClientHandler(socket, this));
            } catch (IOException e) {
                if (!running || serverSocket.isClosed()) break;
                listener.onLog("Accept error: " + e.getMessage());
            }
        }
    }

    /** Validates the requested name, replies OK to the client, then adds it to the active list. */
    synchronized void register(ClientHandler handler, String requested) {
        String name;
        boolean readOnly = requested.isEmpty();
        if (readOnly) {
            name = "Guest-" + guestCounter.incrementAndGet() + " (read-only)";
        } else {
            if (!VALID_NAME.matcher(requested).matches()) {
                throw new IllegalArgumentException("Invalid username (1-20 letters, digits, '_', '-' or '.')");
            }
            name = requested;
            for (ClientHandler c : clients) {
                if (c.getUsername().equalsIgnoreCase(name)) {
                    throw new IllegalArgumentException("Username '" + name + "' is already in use");
                }
            }
        }
        // OK is sent *before* the handler becomes visible to broadcasts, so it is always the first reply.
        handler.accept(name, readOnly);
        clients.add(handler);
    }

    void announceJoin(ClientHandler handler) {
        listener.onLog("Welcome " + handler.getUsername());
        listener.onClientJoined(handler.getUsername());
        broadcast("SYS|" + handler.getUsername() + " joined the chat");
    }

    void unregister(ClientHandler handler) {
        if (clients.remove(handler)) {
            listener.onLog(handler.getUsername() + " disconnected");
            listener.onClientLeft(handler.getUsername());
            broadcast("SYS|" + handler.getUsername() + " left the chat");
        }
    }

    void broadcastChat(String sender, String text) {
        String time = LocalTime.now().format(TIME);
        listener.onLog(sender + ": " + text); // the UI adds its own log timestamp
        broadcast("MSG|" + time + "|" + sender + "|" + text);
    }

    void log(String message) {
        listener.onLog(message);
    }

    private void broadcast(String line) {
        for (ClientHandler h : clients) h.send(line);
    }
}
