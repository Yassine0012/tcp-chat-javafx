package chat.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/** Username screen. Leaving the field empty joins in READ-ONLY MODE. */
public class LoginView {

    private final StackPane root = new StackPane();
    private final TextField usernameField = new TextField();
    private final Button joinButton = new Button("Join chat");
    private final Label messageLabel = new Label();

    public LoginView(String serverAddress) {
        root.getStyleClass().add("login-root");

        Label title = new Label("Group Chat");
        title.getStyleClass().add("title");
        Label server = new Label("Server: " + serverAddress);
        server.getStyleClass().add("muted");
        Label prompt = new Label("Username");
        usernameField.setPromptText("e.g. alice");
        usernameField.setPrefColumnCount(18);
        Label hint = new Label("Leave empty to enter READ-ONLY MODE (you can read but not send).");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        hint.setMaxWidth(320);

        joinButton.setDefaultButton(true);
        joinButton.getStyleClass().add("primary-button");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        usernameField.textProperty().addListener((o, a, text) ->
                joinButton.setText(text.isBlank() ? "Join read-only" : "Join chat"));

        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(320);

        GridPane card = new GridPane();
        card.getStyleClass().add("card");
        card.setVgap(10);
        card.setPadding(new Insets(28));
        card.setMaxSize(GridPane.USE_PREF_SIZE, GridPane.USE_PREF_SIZE);
        card.add(title, 0, 0);
        card.add(server, 0, 1);
        card.add(prompt, 0, 2);
        card.add(usernameField, 0, 3);
        card.add(hint, 0, 4);
        card.add(joinButton, 0, 5);
        card.add(messageLabel, 0, 6);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);

        HBox.setHgrow(card, Priority.NEVER);
        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    public StackPane getRoot() { return root; }

    public TextField getUsernameField() { return usernameField; }

    public Button getJoinButton() { return joinButton; }

    public String getUsername() { return usernameField.getText().trim(); }

    public void setBusy(boolean busy) {
        joinButton.setDisable(busy);
        usernameField.setDisable(busy);
        if (busy) showInfo("Connecting...");
    }

    public void showError(String text) {
        messageLabel.getStyleClass().setAll("error-text");
        messageLabel.setText(text);
    }

    public void showInfo(String text) {
        messageLabel.getStyleClass().setAll("muted");
        messageLabel.setText(text);
    }

    public void focusUsername() { usernameField.requestFocus(); }
}
