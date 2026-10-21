package chat.model;

import java.util.List;

/**
 * Events emitted by {@link ChatClient}. The model has no dependency on JavaFX; any UI (or test)
 * can implement this. Callbacks come from the network reader thread.
 */
public interface ClientListener {
    void onChatMessage(String time, String sender, String text);

    void onSystemMessage(String text);

    void onUserList(List<String> users);

    void onError(String text);

    /** Called exactly once when the connection is over (user left, server stopped, network error). */
    void onDisconnected(String reason);
}
