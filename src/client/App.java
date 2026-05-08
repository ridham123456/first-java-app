package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import server.MyServer;
import server.QuizEngine;

import java.io.*;
import javafx.scene.control.Label;
import java.net.*;
import java.util.*;

public class App extends Application {

    // ── Server connection ────────────────────────────────────────────────────
    private PrintWriter serverOut;
    private BufferedReader serverIn;
    private Thread listenerThread;
    private volatile boolean connected = false;

    // ── Styles ───────────────────────────────────────────────────────────────
    private static final String BG = "-fx-background-color: #0f172a;";
    private static final String C_LIME = "#deff9a";
    private static final String C_GREEN = "#22c55e";
    private static final String C_DARK = "#1e293b";
    private static final String C_SLATE = "#94a3b8";

    private static final String STYLE_PRIMARY
            = "-fx-background-color: " + C_LIME + "; -fx-text-fill: #1a1a1a; -fx-font-size: 22px; "
            + "-fx-font-weight: bold; -fx-padding: 15 45; -fx-background-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_SECONDARY
            = "-fx-background-color: transparent; -fx-text-fill: " + C_LIME + "; -fx-font-size: 22px; "
            + "-fx-font-weight: bold; -fx-padding: 15 45; -fx-border-color: " + C_LIME + "; "
            + "-fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;";

    private static final String STYLE_MODE_BTN
            = "-fx-background-color: " + C_DARK + "; -fx-text-fill: " + C_LIME + "; -fx-font-size: 24px; "
            + "-fx-font-weight: bold; -fx-padding: 20 60; -fx-border-color: " + C_LIME + "; "
            + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_FIELD
            = "-fx-background-color: " + C_DARK + "; -fx-text-fill: white; "
            + "-fx-font-size: 16px; -fx-padding: 12; -fx-background-radius: 8;";

    private static final String STYLE_LINK
            = "-fx-background-color: transparent; -fx-text-fill: " + C_SLATE + "; "
            + "-fx-font-size: 16px; -fx-cursor: hand; -fx-underline: true;";

    // ── Quiz data ────────────────────────────────────────────────────────────
    private final List<QuizEngine.Question> collectedQuestions = new ArrayList<>();
    private String selectedMode;
    private int totalQue;
    private int timeVal;
    private int pointsPerQuestion;

    // ── UI refs ──────────────────────────────────────────────────────────────
    private Label countLabel;
    private Stage primaryStage;

    // ── Launch ───────────────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Single BorderPane wrapper — ek j vaar banay, bar bar nahi
        BorderPane wrapper = new BorderPane();
        wrapper.setStyle(BG);
        Scene scene = new Scene(wrapper, 1000, 720);
        primaryStage.setScene(scene);
        primaryStage.setTitle("HyperSync v1.0");
        primaryStage.setMaximized(true);
        primaryStage.show();
        primaryStage.setFullScreen(true);

        // Server start karo
        if (MyServer.INSTANCE == null) {
            new Thread(() -> MyServer.main(null)).start();
        } else {
            System.out.println("Address already in use: bind");
        }
        // Server ready thay pachhi connect karo
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            connectToServer();
        }).start();

        showMainMenu();
    }

    // ── Server connection ────────────────────────────────────────────────────
    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5555);
            serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            connected = true;
            serverOut.println(MyServer.CMD_JOIN + "TEACHER:Host");
            startServerListener();
        } catch (IOException e) {
            System.err.println("[Teacher] Server connect error: " + e.getMessage());
        }
    }

    private void startServerListener() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = serverIn.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (IOException ignored) {
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith(MyServer.MSG_STUDENT_LIST)) {
            String names = msg.substring(MyServer.MSG_STUDENT_LIST.length());
            int count = names.trim().isEmpty() ? 0 : names.split(",").length;
            Platform.runLater(() -> {
                if (countLabel != null) {
                    countLabel.setText(count + " Student" + (count != 1 ? "s" : "") + " Joined");
                }
            });
        } else if (msg.startsWith(MyServer.MSG_QUIZ_END)) {
            String json = msg.substring(MyServer.MSG_QUIZ_END.length());
            Platform.runLater(() -> showScoreboard(json, true));
        }
    }

    private void sendToServer(String msg) {
        if (connected && serverOut != null) {
            serverOut.println(msg);
        }
    }

    // ── setScene — sirf center swap thay, window flicker nahi thay ──────────
    // આ મેથડ એક જ વાર રાખવી
    private void setScene(VBox root) {
        Platform.runLater(() -> {
            root.setStyle(BG + root.getStyle());

            // જો primaryStage ના સીનનો રૂટ BorderPane હોય તો જ સેન્ટર સેટ થશે
            if (primaryStage.getScene().getRoot() instanceof BorderPane) {
                BorderPane wrapper = (BorderPane) primaryStage.getScene().getRoot();
                wrapper.setCenter(root);
            }

            // ફુલ સ્ક્રીન જાળવી રાખવા માટે (જો જરૂર હોય તો)
        });
    }

    // ── Screens ──────────────────────────────────────────────────────────────
    private void showMainMenu() {
        Label title = styledLabel("HYPERSYNC", "70px", C_LIME, true);
        Label subtitle = styledLabel("High-Performance Real-time Quiz", "20px", "white", false);
        subtitle.setOpacity(0.7);
        subtitle.setPadding(new Insets(0, 0, 50, 0));

        Button btnCreate = new Button("Create Game");
        Button btnJoin = new Button("Join Game (Student)");
        btnCreate.setStyle(STYLE_PRIMARY);
        btnJoin.setStyle(STYLE_SECONDARY);

        btnCreate.setOnAction(e -> showTeacherDashboard());
        btnJoin.setOnAction(e -> showJoinPopup());

        HBox buttons = new HBox(30, btnCreate, btnJoin);
        buttons.setAlignment(Pos.CENTER);

        VBox root = centeredVBox(15, title, subtitle, buttons);
        setScene(root);
    }

    private void showTeacherDashboard() {
        Label head = styledLabel("SELECT GAME MODE", "35px", C_LIME, true);
        head.setPadding(new Insets(0, 0, 20, 0));

        Button btnMarathon = new Button("MARATHON MODE");
        Button btnRapidFire = new Button("RAPID FIRE MODE");
        btnMarathon.setStyle(STYLE_MODE_BTN);
        btnRapidFire.setStyle(STYLE_MODE_BTN);

        addHover(btnMarathon, STYLE_MODE_BTN);
        addHover(btnRapidFire, STYLE_MODE_BTN);

        Label descMarathon = descLabel("Focus on accuracy. More time per question.");
        Label descRapidFire = descLabel("Focus on speed. Very limited time (30-60 sec).");

        btnMarathon.setOnAction(e -> showModeSettings("Marathon"));
        btnRapidFire.setOnAction(e -> showModeSettings("Rapid Fire"));

        VBox marathonBox = new VBox(10, btnMarathon, descMarathon);
        VBox rapidFireBox = new VBox(10, btnRapidFire, descRapidFire);
        marathonBox.setAlignment(Pos.CENTER);
        rapidFireBox.setAlignment(Pos.CENTER);

        HBox modes = new HBox(50, marathonBox, rapidFireBox);
        modes.setAlignment(Pos.CENTER);

        Button btnBack = backButton(this::showMainMenu);
        VBox root = centeredVBox(30, head, modes, btnBack);
        setScene(root);
    }

    private void showModeSettings(String mode) {
        selectedMode = mode;

        Label head = styledLabel(mode.toUpperCase() + " CONFIGURATION", "32px", C_LIME, true);
        head.setPadding(new Insets(0, 0, 10, 0));

        TextField queField = field("Total Number of Questions");
        TextField timeField = field(mode.equalsIgnoreCase("Marathon")
                ? "Total Quiz Time (in minutes)" : "Time per Question (in seconds)");
        TextField pointsField
                = field("Points Per Question");

        Button btnNext = new Button("NEXT");
        btnNext.setStyle("-fx-background-color: " + C_GREEN + "; -fx-text-fill: white; "
                + "-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 60; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");

        Label errLabel = styledLabel("", "14px", "#f87171", false);

        btnNext.setOnAction(e -> {
            try {
                totalQue = Integer.parseInt(queField.getText().trim());
                int t = Integer.parseInt(timeField.getText().trim());
                timeVal = mode.equalsIgnoreCase("Marathon") ? t * 60 : t;
                pointsPerQuestion
                        = Integer.parseInt(
                                pointsField.getText().trim()
                        );
                if (totalQue <= 0 || t <= 0) {
                    throw new NumberFormatException();
                }
                collectedQuestions.clear();
                showQuestionEntry(1);
            } catch (NumberFormatException ex) {
                errLabel.setText("Please enter valid positive numbers.");
            }
        });

        Button btnBack = backButton(this::showTeacherDashboard);
        Label dummy = new Label();
        dummy.setFocusTraversable(true);

        VBox root = centeredVBox(
    25,
    dummy,
    head,
    queField,
    timeField,
    pointsField,
    errLabel,
    btnNext,
    btnBack
);
        setScene(root);
        dummy.requestFocus();
    }

    private void showQuestionEntry(int qNum) {
        Label head = styledLabel("QUESTION " + qNum + " OF " + totalQue, "28px", C_LIME, true);

        TextArea queArea = new TextArea();
        queArea.setPromptText("Enter your question here...");
        queArea.setMaxSize(600, 100);
        queArea.setStyle("-fx-control-inner-background: " + C_DARK + "; "
                + "-fx-text-fill: white; -fx-font-size: 16px;");

        TextField opt1 = field("Option A");
        TextField opt2 = field("Option B");
        TextField opt3 = field("Option C");
        TextField opt4 = field("Option D");

        ChoiceBox<String> correctBox = new ChoiceBox<>();
        correctBox.getItems().addAll("A", "B", "C", "D");
        correctBox.setValue("A ");
        //correctBox.setStyle("-fx-background-color: " + C_DARK + "; -fx-text-fill: white;");
        //correctBox.setStyle("-fx-background-color: " + C_DARK + ";" +"-fx-mark-color: white;" +"-fx-font-size: 16px;");
        correctBox.setStyle(
                "-fx-background-color: rgb(242, 245, 248);"
                + "-fx-mark-color: white;"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-inner-color: white;"
        );
        Label correctLabel = styledLabel("Select Correct Option:", "14px", "white", false);

        boolean isLast = (qNum == totalQue);
        Button btnNext = new Button(isLast ? "FINISH & LAUNCH" : "NEXT QUESTION");
        btnNext.setStyle("-fx-background-color: " + C_GREEN + "; -fx-text-fill: white; "
                + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12 40; -fx-cursor: hand;");

        Label errLabel = styledLabel("", "14px", "#f87171", false);

        btnNext.setOnAction(e -> {
            String qText = queArea.getText().trim();
            String a = opt1.getText().trim();
            String b = opt2.getText().trim();
            String c = opt3.getText().trim();
            String d = opt4.getText().trim();

            if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
                errLabel.setText("Please fill in the question and all 4 options.");
                return;
            }

            collectedQuestions.add(new QuizEngine.Question(qText, a, b, c, d, correctBox.getValue().trim()));

            if (qNum < totalQue) {
                showQuestionEntry(qNum + 1);
            } else {
                finalizeAndSetupServer();
                showWaitingLobby();
            }
        });

        HBox correctRow = new HBox(10, correctLabel, correctBox);
        correctRow.setAlignment(Pos.CENTER);

        VBox root = centeredVBox(15, head, queArea, opt1, opt2, opt3, opt4, correctRow, errLabel, btnNext);
        root.setPadding(new Insets(30));
        setScene(root);
    }

    private void finalizeAndSetupServer() {
        if (MyServer.INSTANCE != null) {
            MyServer.INSTANCE.setupQuiz(selectedMode, totalQue, timeVal,pointsPerQuestion,collectedQuestions);
        }
    }

    private void showWaitingLobby() {
        String ip = "localhost";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        Label modeLabel = styledLabel("MODE: " + selectedMode.toUpperCase(), "20px", C_LIME, false);
        Label statusLabel = styledLabel("WAITING FOR STUDENTS...", "35px", "white", true);
        countLabel = styledLabel("0 Students Joined", "24px", C_GREEN, false);

        Label ipLabel = styledLabel("Server IP:  " + ip + "  |  Port: 5555", "18px", C_LIME, true);
        Label settingsLabel = styledLabel(
                totalQue + " Questions  |  " + (selectedMode.equalsIgnoreCase("Marathon")
                ? (timeVal / 60 + " min total") : (timeVal + " sec / question")),
                "16px", C_SLATE, false);

        Button btnStart = new Button("START QUIZ NOW");
        btnStart.setStyle("-fx-background-color: " + C_LIME + "; -fx-text-fill: #1a1a1a; "
                + "-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 50; -fx-cursor: hand;");

        btnStart.setOnAction(e -> {
            sendToServer(MyServer.CMD_START);
            showTeacherLiveView();
        });

        VBox root = centeredVBox(25, modeLabel, statusLabel, countLabel, ipLabel, settingsLabel, btnStart);
        setScene(root);
    }

    private void showTeacherLiveView() {
        Label head = styledLabel("QUIZ IN PROGRESS", "30px", C_LIME, true);
        Label subLabel = styledLabel("Students are answering live...", "16px", C_SLATE, false);
        Label info = styledLabel("Final scoreboard will appear automatically when quiz ends.", "14px", C_SLATE, false);

        VBox root = centeredVBox(20, head, subLabel, info);
        setScene(root);
    }

    private void showScoreboard(String json, boolean isFinal) {
        Label head = styledLabel(isFinal ? "FINAL SCOREBOARD" : "LIVE SCORES", "40px", C_LIME, true);
        head.setPadding(new Insets(0, 0, 20, 0));

        VBox scoreList = new VBox(10);
        scoreList.setAlignment(Pos.CENTER);

        List<String[]> entries = parseScoresJson(json);
        String[] medals = {"1st", "2nd", "3rd"};

        for (int i = 0; i < entries.size(); i++) {
            String[] entry = entries.get(i);
            String rank = (i < 3) ? medals[i] : ("#" + (i + 1));
            String name = entry[0];
            String pts = entry[1] + " pts";
            long timeMs = Long.parseLong(entry[2]);

            boolean hasTie = isFinal && i + 1 < entries.size()
                    && entries.get(i)[1].equals(entries.get(i + 1)[1]);

            String rowColor = hasTie ? "#fbbf24" : "white";
            String tieNote = hasTie ? "  [Tiebreak by time]" : "";

            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(560);
            row.setPadding(new Insets(14, 20, 14, 20));
            row.setStyle("-fx-background-color: " + C_DARK + "; -fx-background-radius: 10;"
                    + (hasTie ? " -fx-border-color: #fbbf24; -fx-border-width: 2; -fx-border-radius: 10;" : ""));

            Label rankL = styledLabel(rank, "18px", rowColor, true);
            rankL.setMinWidth(55);

            VBox nameBox = new VBox(3);
            Label nameL = styledLabel(name + tieNote, "18px", rowColor, true);
            nameBox.getChildren().add(nameL);
            if (isFinal) {
                Label timeL = styledLabel("Answer time: " + (timeMs / 1000.0) + "s", "12px", C_SLATE, false);
                nameBox.getChildren().add(timeL);
            }
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            Label ptsL = styledLabel(pts, "16px", C_SLATE, false);
            row.getChildren().addAll(rankL, nameBox, ptsL);
            scoreList.getChildren().add(row);
        }

        Button btnMenu = new Button("Back to Main Menu");
        btnMenu.setStyle(STYLE_PRIMARY);
        btnMenu.setOnAction(e -> showMainMenu());

        ScrollPane scroll = new ScrollPane(scoreList);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(500);

        VBox root = centeredVBox(20, head, scroll, btnMenu);
        root.setPadding(new Insets(30));
        setScene(root);
    }

    private void showJoinPopup() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Students run MyClient.java on their machine on the same WiFi/LAN.\n\n"
                + "They enter your IP shown on the lobby screen.",
                ButtonType.OK);
        alert.setTitle("Join Game");
        alert.setHeaderText("Students run MyClient.java separately");
        alert.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private VBox centeredVBox(int spacing, javafx.scene.Node... nodes) {
        VBox box = new VBox(spacing);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label styledLabel(String text, String size, String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "; -fx-text-fill: " + color + ";"
                + (bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }

    private Label descLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-opacity: 0.6;");
        return l;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(350);
        tf.setStyle(STYLE_FIELD);
        return tf;
    }

    private Button backButton(Runnable action) {
        Button b = new Button("Back");
        b.setStyle(STYLE_LINK);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void addHover(Button btn, String base) {
        btn.setOnMouseEntered(e -> btn.setStyle(base + "-fx-background-color: #334155;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private List<String[]> parseScoresJson(String json) {

        List<String[]> result = new ArrayList<>();

        json = json.trim();

        if (json.startsWith("[")) {
            json = json.substring(1);
        }

        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }

        String[] entries = json.split("\\},\\{");

        for (String entry : entries) {

            entry = entry.replace("{", "")
                    .replace("}", "");

            String name = extractJsonValue(entry, "name");
            String points = extractJsonValue(entry, "points");
            String timeMs = extractJsonValue(entry, "timeMs");

            if (name != null && points != null) {

                result.add(new String[]{
                    name,
                    points,
                    timeMs != null ? timeMs : "0"
                });
            }
        }

        return result;
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        String rest = json.substring(idx + pattern.length()).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf("\"", 1);
            return end > 0 ? rest.substring(1, end) : null;
        } else {
            int end = rest.indexOf(",");
            if (end < 0) {
                end = rest.indexOf("}");
            }
            return end > 0 ? rest.substring(0, end).trim() : rest.trim();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
