package chat.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the server model with plain sockets. Because the model has no JavaFX dependency,
 * these tests run headless (no display needed), which demonstrates the Model/View separation.
 */
class ChatServerTest {

    private ChatServer server;
    private final List<String> logs = new CopyOnWriteArrayList<>();
    private final List<String> joined = new CopyOnWriteArrayList<>();
    private final List<String> left = new CopyOnWriteArrayList<>();

    /** Minimal raw-socket client speaking the line protocol. */
    private static final class Raw implements AutoCloseable {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;

        Raw(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(3000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        }

        void send(String line) { out.println(line); }

        String read() throws IOException { return in.readLine(); }

        /** Reads lines until one starts with the prefix (skips welcome / join notices). */
        String readUntil(String prefix) throws IOException {
            for (int i = 0; i < 500; i++) {
                String l = read();
                if (l == null) fail("connection closed while waiting for " + prefix);
                if (l.startsWith(prefix)) return l;
            }
            return fail("did not receive " + prefix);
        }

        @Override public void close() throws IOException { socket.close(); }
    }

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer("127.0.0.1", 0, new ServerListener() {
            public void onLog(String m) { logs.add(m); }
            public void onClientJoined(String u) { joined.add(u); }
            public void onClientLeft(String u) { left.add(u); }
        });
        server.start();
    }

    @AfterEach
    void stopServer() { server.stop(); }

    @Test
    void joinWithUsernameIsAcceptedAndLogged() throws Exception {
        try (Raw alice = new Raw(server.getLocalPort())) {
            alice.send("JOIN|alice");
            assertEquals("OK|alice|false", alice.read());
            assertTrue(alice.readUntil("SYS|Welcome").contains("alice"));
            alice.readUntil("SYS|alice joined");
            assertTrue(logs.contains("Server Started on 127.0.0.1:" + server.getLocalPort()));
            assertTrue(logs.contains("Welcome alice"));
            assertEquals(List.of("alice"), joined);
        }
    }

    @Test
    void messageIsBroadcastToEveryoneWithSenderAndTime() throws Exception {
        try (Raw a = new Raw(server.getLocalPort()); Raw b = new Raw(server.getLocalPort())) {
            a.send("JOIN|alice");
            assertEquals("OK|alice|false", a.read());
            b.send("JOIN|bob");
            assertEquals("OK|bob|false", b.read());
            b.readUntil("SYS|bob joined");

            a.send("MSG|hello everyone");
            String forA = a.readUntil("MSG|");
            String forB = b.readUntil("MSG|");
            assertEquals(forA, forB);
            String[] p = forB.split("\\|", 4);
            assertTrue(p[1].matches("\\d\\d:\\d\\d:\\d\\d"), "time stamp");
            assertEquals("alice", p[2]);
            assertEquals("hello everyone", p[3]);
        }
    }

    @Test
    void emptyUsernameMeansReadOnlyAndSendingIsRefused() throws Exception {
        try (Raw guest = new Raw(server.getLocalPort())) {
            guest.send("JOIN|");
            String ok = guest.read();
            assertTrue(ok.startsWith("OK|Guest-") && ok.endsWith("|true"), ok);
            guest.send("MSG|can I talk?");
            assertTrue(guest.readUntil("ERR|").contains("READ-ONLY"));
        }
    }

    @Test
    void duplicateUsernameIsRejectedCaseInsensitively() throws Exception {
        try (Raw a = new Raw(server.getLocalPort()); Raw b = new Raw(server.getLocalPort())) {
            a.send("JOIN|alice");
            assertEquals("OK|alice|false", a.read());
            b.send("JOIN|ALICE");
            assertTrue(b.read().startsWith("ERR|"));
        }
    }

    @Test
    void invalidUsernameIsRejected() throws Exception {
        try (Raw a = new Raw(server.getLocalPort())) {
            a.send("JOIN|bad|name");
            assertTrue(a.read().startsWith("ERR|"));
        }
    }

    @Test
    void allUsersIsAnsweredOnlyToTheRequester() throws Exception {
        try (Raw a = new Raw(server.getLocalPort()); Raw b = new Raw(server.getLocalPort())) {
            a.send("JOIN|alice");
            assertEquals("OK|alice|false", a.read());
            b.send("JOIN|bob");
            assertEquals("OK|bob|false", b.read());
            b.readUntil("SYS|bob joined");
            a.readUntil("SYS|bob joined");

            a.send("ALLUSERS");
            assertEquals("USERS|alice,bob", a.readUntil("USERS|"));

            // bob must not have received a USERS line: next thing he sees is his own chat message
            b.send("MSG|ping");
            assertTrue(b.read().startsWith("MSG|"));
        }
    }

    @Test
    void byeRemovesClientAndNotifiesOthers() throws Exception {
        try (Raw a = new Raw(server.getLocalPort()); Raw b = new Raw(server.getLocalPort())) {
            a.send("JOIN|alice");
            a.read();
            b.send("JOIN|bob");
            b.read();
            a.readUntil("SYS|bob joined");

            b.send("BYE");
            assertEquals("SYS|bob left the chat", a.readUntil("SYS|bob left"));
            assertTrue(left.contains("bob"));
            assertEquals(List.of("alice"), server.userNames());
        }
    }

    @Test
    void manyConcurrentClientsAreServed() throws Exception {
        int n = 50;
        List<Raw> clients = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                Raw r = new Raw(server.getLocalPort());
                r.send("JOIN|user" + i);
                assertEquals("OK|user" + i + "|false", r.read());
                clients.add(r);
            }
            // OK is sent just before the handler is added to the list, so wait for the count to settle
            long deadline = System.currentTimeMillis() + 3000;
            while (server.userNames().size() < n && System.currentTimeMillis() < deadline) Thread.sleep(10);
            assertEquals(n, server.userNames().size());
            clients.get(0).send("MSG|broadcast");
            for (Raw r : clients) assertTrue(r.readUntil("MSG|").endsWith("|user0|broadcast"));
        } finally {
            for (Raw r : clients) r.close();
        }
    }
}
