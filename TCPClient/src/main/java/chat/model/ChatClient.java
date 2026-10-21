package chat.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure-Java chat client (the "Model"): owns the socket, speaks the line protocol described in
 * {@code ChatServer} and reports everything through a {@link ClientListener}. No JavaFX here.
 */
public class ChatClient {

    private final String host;
    private final int port;
    private final ClientListener listener;
    private final AtomicBoolean finished = new AtomicBoolean();

    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile boolean connected;
    private volatile boolean leavingOnPurpose;
    private volatile String username = "";
    private volatile boolean readOnly;

    public ChatClient(String host, int port, ClientListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    /**
     * Opens the connection and performs the JOIN handshake. Blocks, so call it off the UI thread.
     *
     * @param requestedUsername blank = enter READ-ONLY MODE
     * @throws IOException if the server is unreachable or rejects the username
     */
    public void connect(String requestedUsername) throws IOException {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(5000); // only for the handshake
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            w.println("JOIN|" + requestedUsername.trim());
            String reply = in.readLine();
            if (reply == null) throw new IOException("Server closed the connection");
            if (reply.startsWith("ERR|")) throw new IOException(reply.substring(4));
            String[] parts = reply.split("\\|", 3);
            if (parts.length < 3 || !parts[0].equals("OK")) throw new IOException("Unexpected reply from server");

            s.setSoTimeout(0);
            username = parts[1];
            readOnly = Boolean.parseBoolean(parts[2]);
            socket = s;
            out = w;
            connected = true;
            Thread.ofVirtual().name("client-reader").start(() -> readLoop(in));
        } catch (IOException | RuntimeException e) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Interprets what the user typed: {@code end}/{@code bye} leave the chat, {@code allUsers} asks
     * the server for the active users, anything else is a chat message.
     */
    public void processInput(String input) {
        String text = input.replaceAll("\\R", " ").trim();
        if (text.isEmpty() || !connected) return;

        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            disconnect();
        } else if (text.equalsIgnoreCase("allUsers")) {
            send("ALLUSERS");
        } else if (readOnly) {
            listener.onError("You are in READ-ONLY MODE and cannot send messages");
        } else {
            send("MSG|" + text);
        }
    }

    /** Tells the server goodbye and closes the connection (safe to call more than once). */
    public void disconnect() {
        if (!connected) return;
        leavingOnPurpose = true;
        send("BYE");
        closeSocket();
    }

    // ---------------------------------------------------------------- internals

    private synchronized void send(String line) {
        PrintWriter w = out;
        if (w != null) w.println(line);
    }

    private void closeSocket() {
        try {
            Socket s = socket;
            if (s != null) s.close();
        } catch (IOException ignored) {
        }
    }

    private void readLoop(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) dispatch(line);
        } catch (IOException ignored) {
            // socket closed or network failure
        } finally {
            connected = false;
            closeSocket();
            if (finished.compareAndSet(false, true)) {
                listener.onDisconnected(leavingOnPurpose ? "You left the chat" : "Connection to the server was lost");
            }
        }
    }

    private void dispatch(String line) {
        if (line.startsWith("MSG|")) {
            String[] p = line.split("\\|", 4);
            if (p.length == 4) listener.onChatMessage(p[1], p[2], p[3]);
        } else if (line.startsWith("SYS|")) {
            listener.onSystemMessage(line.substring(4));
        } else if (line.startsWith("USERS|")) {
            String csv = line.substring(6);
            List<String> users = csv.isEmpty() ? List.of() : Arrays.asList(csv.split(","));
            listener.onUserList(users);
        } else if (line.startsWith("ERR|")) {
            listener.onError(line.substring(4));
        }
    }
}
