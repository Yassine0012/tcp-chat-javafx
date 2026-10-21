package chat.controller;

import chat.config.ClientConfig;
import chat.model.ChatClient;
import chat.model.ClientListener;
import chat.view.ChatLine;
import chat.view.ChatView;
import chat.view.LoginView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Glue between the views and {@link ChatClient}. Implements {@link ClientListener}; every network
 * event is moved to the JavaFX thread before it touches a view. Socket writes happen on a single
 * background thread so the UI never blocks and messages keep their order.
 */
public final class ClientController implements ClientListener {

    private final ClientConfig config;
    private final Scene scene;
    private final LoginView loginView;
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "client-sender");
        t.setDaemon(true);
        return t;
    });

    private ChatClient client;
    private ChatView chatView;

    public ClientController(ClientConfig config, Scene scene) {
        this.config = config;
        this.scene = scene;
        this.loginView = new LoginView(config.host() + ":" + config.port());
        loginView.getJoinButton().setOnAction(e -> join());
        showRoot(loginView.getRoot());
        Platform.runLater(loginView::focusUsername);
    }

    // ---------------------------------------------------------------- user actions

    private void join() {
        String requested = loginView.getUsername();
        loginView.setBusy(true);

        chatView = new ChatView();
        chatView.getSendButton().setOnAction(e -> send());
        chatView.getLeaveButton().setOnAction(e -> leave());
        ChatClient c = new ChatClient(config.host(), config.port(), this);
        client = c;

        Thread.ofVirtual().start(() -> {
            try {
                c.connect(requested);
                Platform.runLater(() -> {
                    chatView.setUser(c.getUsername(), c.isReadOnly());
                    showRoot(chatView.getRoot());
                    if (!c.isReadOnly()) chatView.requestInputFocus();
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    loginView.setBusy(false);
                    loginView.showError("Cannot join: " + ex.getMessage());
                });
            }
        });
    }

    private void send() {
        String text = chatView.takeInput();
        if (text.isBlank()) return;
        ChatClient c = client;
        sender.execute(() -> c.processInput(text));
    }

    private void leave() {
        ChatClient c = client;
        sender.execute(c::disconnect);
    }

    /** Called when the window is closed. */
    public void shutdown() {
        ChatClient c = client;
        if (c != null) c.disconnect();
        sender.shutdown();
    }

    // ---------------------------------------------------------------- ClientListener

    @Override
    public void onChatMessage(String time, String senderName, String text) {
        boolean own = client != null && senderName.equals(client.getUsername());
        ChatLine line = new ChatLine(own ? ChatLine.Kind.OWN : ChatLine.Kind.OTHER,
                "[" + time + "] " + (own ? "You" : senderName) + ": " + text);
        Platform.runLater(() -> chatView.append(line));
    }

    @Override
    public void onSystemMessage(String text) {
        Platform.runLater(() -> chatView.append(new ChatLine(ChatLine.Kind.SYSTEM, text)));
    }

    @Override
    public void onUserList(List<String> users) {
        String text = "Active users (" + users.size() + "): " + String.join(", ", users);
        Platform.runLater(() -> chatView.append(new ChatLine(ChatLine.Kind.SYSTEM, text)));
    }

    @Override
    public void onError(String text) {
        Platform.runLater(() -> chatView.append(new ChatLine(ChatLine.Kind.ERROR, text)));
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            loginView.setBusy(false);
            loginView.showInfo("Disconnected: " + reason);
            showRoot(loginView.getRoot());
            loginView.focusUsername();
        });
    }

    private void showRoot(Region root) {
        scene.setRoot(root);
    }
}
