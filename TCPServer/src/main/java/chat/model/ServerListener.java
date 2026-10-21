package chat.model;

/**
 * Callback interface through which the model reports events. The model knows nothing about
 * JavaFX: any front-end (JavaFX, console, tests) can implement this interface.
 * Callbacks are invoked from server threads, so UI implementations must hop to their own thread.
 */
public interface ServerListener {
    void onLog(String message);

    void onClientJoined(String username);

    void onClientLeft(String username);
}
