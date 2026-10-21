package chat.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Serves exactly one client connection; each instance runs on its own virtual thread. */
final class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private volatile PrintWriter out;
    private volatile String username = "?";
    private volatile boolean readOnly;

    ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    String getUsername() {
        return username;
    }

    void accept(String name, boolean readOnly) {
        this.username = name;
        this.readOnly = readOnly;
        send("OK|" + name + "|" + readOnly);
    }

    synchronized void send(String line) {
        PrintWriter w = out;
        if (w != null) w.println(line);
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        Socket s = socket;
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
            this.out = w;

            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN|")) {
                send("ERR|Expected JOIN");
                return;
            }
            try {
                server.register(this, first.substring(5).trim());
            } catch (IllegalArgumentException e) {
                server.log("Rejected client: " + e.getMessage());
                send("ERR|" + e.getMessage());
                return;
            }
            send("SYS|Welcome " + username + "!" + (readOnly ? " You are in READ-ONLY MODE." : ""));
            server.announceJoin(this);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MSG|")) {
                    String text = line.substring(4).trim();
                    if (readOnly) send("ERR|You are in READ-ONLY MODE and cannot send messages");
                    else if (!text.isEmpty()) server.broadcastChat(username, text);
                } else if (line.equals("ALLUSERS")) {
                    send("USERS|" + String.join(",", server.userNames()));
                } else if (line.equals("BYE")) {
                    break;
                } else {
                    send("ERR|Unknown command");
                }
            }
        } catch (IOException e) {
            // connection dropped or server stopping: fall through to cleanup
        } finally {
            server.unregister(this);
        }
    }
}
