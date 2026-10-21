package chat.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The server window layout (pure presentation, built on a {@link GridPane}).
 * It knows nothing about sockets: the controller feeds it text and user names.
 */
public class ServerView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GridPane root = new GridPane();
    private final ListView<String> logView = new ListView<>();
    private final ListView<String> usersView = new ListView<>();
    private final Label statusLabel = new Label("Stopped");
    private final Circle statusDot = new Circle(6, Color.GRAY);
    private final Label countLabel = new Label("Connected clients: 0");

    private final Map<String, Color> userColors = new HashMap<>();
    private final Random random = new Random();

    public ServerView() {
        root.getStyleClass().add("server-root");
        root.setPadding(new Insets(16));
        root.setHgap(12);
        root.setVgap(8);

        Label title = new Label("Chat Server");
        title.getStyleClass().add("title");
        statusLabel.getStyleClass().add("status-label");
        HBox status = new HBox(6, statusDot, statusLabel);
        status.setAlignment(Pos.CENTER_RIGHT);

        Label logTitle = new Label("Activity log");
        logTitle.getStyleClass().add("section-title");
        Label usersTitle = new Label("Connected users");
        usersTitle.getStyleClass().add("section-title");

        logView.getStyleClass().add("log-view");
        usersView.getStyleClass().add("users-view");
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setWrapText(true);
                setPrefWidth(0); // wrap long lines instead of showing a horizontal scrollbar
            }
        });
        usersView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    // each user gets a random (but stable) background colour
                    setStyle("-fx-background-color: " + toRgb(colorFor(item)) + "; -fx-text-fill: #1f2933;");
                }
            }
        });

        countLabel.getStyleClass().add("footer-label");

        root.add(title, 0, 0);
        root.add(status, 1, 0);
        root.add(logTitle, 0, 1);
        root.add(usersTitle, 1, 1);
        root.add(logView, 0, 2);
        root.add(usersView, 1, 2);
        root.add(countLabel, 0, 3, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(65);
        c0.setHgrow(Priority.ALWAYS);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(35);
        c1.setHgrow(Priority.ALWAYS);
        root.getColumnConstraints().addAll(c0, c1);

        RowConstraints grow = new RowConstraints();
        grow.setVgrow(Priority.ALWAYS);
        root.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), grow, new RowConstraints());
    }

    public GridPane getRoot() {
        return root;
    }

    public void appendLog(String message) {
        logView.getItems().add("[" + LocalTime.now().format(TIME) + "] " + message);
        logView.scrollTo(logView.getItems().size() - 1);
    }

    public void addUser(String username) {
        usersView.getItems().add(username);
        updateCount();
    }

    public void removeUser(String username) {
        usersView.getItems().remove(username);
        userColors.remove(username);
        updateCount();
    }

    public void setRunning(boolean running, String detail) {
        statusDot.setFill(running ? Color.web("#2ecc71") : Color.web("#e74c3c"));
        statusLabel.setText(detail);
    }

    private void updateCount() {
        countLabel.setText("Connected clients: " + usersView.getItems().size());
    }

    private Color colorFor(String user) {
        return userColors.computeIfAbsent(user, u -> Color.hsb(random.nextDouble() * 360, 0.35, 0.97));
    }

    private static String toRgb(Color c) {
        return String.format("rgb(%d,%d,%d)", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}
