package chat.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Main chat window layout (GridPane): header with status, message list, input row. */
public class ChatView {

    private final GridPane root = new GridPane();
    private final ListView<ChatLine> messages = new ListView<>();
    private final TextArea input = new TextArea();
    private final Button sendButton = new Button("SEND");
    private final Button leaveButton = new Button("LEAVE");
    private final Label userLabel = new Label();
    private final Label modeBadge = new Label("READ-ONLY MODE");
    private final Circle statusDot = new Circle(6, Color.web("#2ecc71"));
    private final Label statusLabel = new Label("Online");

    public ChatView() {
        root.getStyleClass().add("chat-root");
        root.setPadding(new Insets(14));
        root.setHgap(10);
        root.setVgap(10);

        Label title = new Label("Group Chat");
        title.getStyleClass().add("title");
        userLabel.getStyleClass().add("muted");
        modeBadge.getStyleClass().add("readonly-badge");
        modeBadge.setVisible(false);
        modeBadge.setManaged(false);
        statusLabel.getStyleClass().add("online-label");
        leaveButton.getStyleClass().add("danger-button");

        // Only the (possibly long) user label may shrink/ellipsize; everything else keeps its size.
        for (Region r : new Region[]{title, modeBadge, statusLabel, leaveButton}) r.setMinWidth(Region.USE_PREF_SIZE);
        userLabel.setMinWidth(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, userLabel, modeBadge, spacer, statusDot, statusLabel, leaveButton);
        header.setAlignment(Pos.CENTER_LEFT);

        messages.getStyleClass().add("message-list");
        messages.setFocusTraversable(false);
        messages.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatLine item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("msg-own", "msg-other", "msg-system", "msg-error");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.text());
                setWrapText(true);
                setPrefWidth(0); // wrap to the list width instead of growing horizontally
                getStyleClass().add(switch (item.kind()) {
                    case OWN -> "msg-own";
                    case OTHER -> "msg-other";
                    case SYSTEM -> "msg-system";
                    case ERROR -> "msg-error";
                });
            }
        });

        input.setPromptText("Type a message and press Enter...   (commands: allUsers, end, bye)");
        input.setWrapText(true);
        input.setPrefRowCount(2);
        // Enter sends (the controller wires the actual action to the SEND button)
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                sendButton.fire();
            }
        });
        sendButton.getStyleClass().add("primary-button");
        sendButton.setMaxHeight(Double.MAX_VALUE);
        sendButton.setPrefWidth(90);

        root.add(header, 0, 0, 2, 1);
        root.add(messages, 0, 1, 2, 1);
        root.add(input, 0, 2);
        root.add(sendButton, 1, 2);

        ColumnConstraints grow = new ColumnConstraints();
        grow.setHgrow(Priority.ALWAYS);
        root.getColumnConstraints().addAll(grow, new ColumnConstraints());
        RowConstraints rowGrow = new RowConstraints();
        rowGrow.setVgrow(Priority.ALWAYS);
        root.getRowConstraints().addAll(new RowConstraints(), rowGrow, new RowConstraints());
    }

    public GridPane getRoot() { return root; }

    public Button getSendButton() { return sendButton; }

    public Button getLeaveButton() { return leaveButton; }

    public String takeInput() {
        String text = input.getText();
        input.clear();
        return text;
    }

    public void setUser(String username, boolean readOnly) {
        userLabel.setText("signed in as " + username);
        modeBadge.setVisible(readOnly);
        modeBadge.setManaged(readOnly);
        input.setDisable(readOnly);
        sendButton.setDisable(readOnly);
        input.setPromptText(readOnly
                ? "READ-ONLY MODE - you can read messages but not send them"
                : "Type a message and press Enter...   (commands: allUsers, end, bye)");
    }

    public void append(ChatLine line) {
        messages.getItems().add(line);
        messages.scrollTo(messages.getItems().size() - 1);
    }

    public void requestInputFocus() { input.requestFocus(); }
}
