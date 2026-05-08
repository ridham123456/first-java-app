package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import server.MyServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class MyClient extends Application {

    // ── Styles ───────────────────────────────────────────────────────────────
    private static final String BG = "-fx-background-color: #0f172a;";
    private static final String C_LIME = "#deff9a";
    private static final String C_GREEN = "#22c55e";
    private static final String C_RED = "#f87171";
    private static final String C_DARK = "#1e293b";
    private static final String C_SLATE = "#94a3b8";

    private static final String[] OPT_COLORS = {"#3b82f6", "#a855f7", "#f59e0b", "#22c55e"};
    private static final String[] OPT_LETTERS = {"A", "B", "C", "D"};

    // ── State ────────────────────────────────────────────────────────────────
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private String studentName;
    private Stage primaryStage;

    private long questionStartTime;
    private boolean answered = false;
    private volatile boolean timerRunning = false;

    private Label timerLabel;
    private Label waitingCountLabel;

    // ── Launch ───────────────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Single BorderPane wrapper — ek j vaar banay
        BorderPane wrapper = new BorderPane();
        wrapper.setStyle(BG);
        Scene scene = new Scene(wrapper, 1000, 720);
        primaryStage.setScene(scene);
        primaryStage.setTitle("HyperSync — Student");
        primaryStage.setMaximized(true);
        primaryStage.show();

        showConnectScreen();
    }

    // ── setScene — sirf center swap ──────────────────────────────────────────
    private void setScene(VBox root) {
        root.setStyle(BG + root.getStyle());
        BorderPane wrapper = (BorderPane) primaryStage.getScene().getRoot();
        wrapper.setCenter(root);
    }

    // ── Connect Screen ────────────────────────────────────────────────────────
    private void showConnectScreen() {
        Label title = bigLabel("HYPERSYNC", C_LIME);
        Label subtitle = smallLabel("Student Portal", C_SLATE);

        TextField nameField = inputField("Your Name");
        TextField ipField = inputField("Teacher's Server IP  (e.g. 192.168.1.5)");
        Label errLabel = smallLabel("", C_RED);

        Button btnJoin = primaryBtn("JOIN GAME");

        btnJoin.setOnAction(e -> {
            String name = nameField.getText().trim();
            String ip = ipField.getText().trim();

            if (name.isEmpty() || ip.isEmpty()) {
                errLabel.setText("Please enter your name and the server IP.");
                return;
            }

            studentName = name;
            errLabel.setText("Connecting...");

            new Thread(() -> {
                boolean ok = connectToServer(ip, 5555, name);
                Platform.runLater(() -> {
                    if (ok) {
                        showWaitingScreen();
                    } else {
                        errLabel.setText("Could not connect. Check IP and make sure teacher has started the server.");
                    }
                });
            }).start();
        });

        VBox root = centeredVBox(20, title, subtitle, nameField, ipField, errLabel, btnJoin);
        setScene(root);
    }

    private boolean connectToServer(String host, int port, String name) {
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out.println(MyServer.CMD_JOIN + name);
            startListener();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> handleMessage(msg));
                }
            } catch (IOException ignored) {
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // ── Message handler ───────────────────────────────────────────────────────
    private void handleMessage(String msg) {
        if (msg.startsWith(MyServer.MSG_JOINED)) {
            // Join confirmed — already showing waiting screen

        } else if (msg.startsWith(MyServer.MSG_ERROR)) {
            String err = msg.substring(MyServer.MSG_ERROR.length());

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Connection Error");
            alert.setContentText(err);
            alert.showAndWait();
            Platform.exit();
        } else if (msg.startsWith(MyServer.MSG_STUDENT_LIST)) {
            String names = msg.substring(MyServer.MSG_STUDENT_LIST.length());
            int count = names.trim().isEmpty() ? 0 : names.split(",").length;
            if (waitingCountLabel != null) {
                waitingCountLabel.setText(count + " student" + (count != 1 ? "s" : "") + " in lobby");
            }

        } else if (msg.equals(MyServer.MSG_START)) {
            showReadyScreen();

        } else if (msg.startsWith(MyServer.MSG_QUESTION)) {
            String json = msg.substring(MyServer.MSG_QUESTION.length());
            Map<String, String> q = parseJson(json);
            answered = false;
            questionStartTime = System.currentTimeMillis();
            showQuestionScreen(q);

        } else if (msg.startsWith(MyServer.MSG_TIMER)) {

            int secs
                    = Integer.parseInt(
                            msg.substring(MyServer.MSG_TIMER.length())
                    );

            timerRunning = false;
            timerRunning = true;

            new Thread(() -> {

                for (int t = secs; t >= 0 && timerRunning; t--) {

                    final int time = t;

                    Platform.runLater(() -> {

                        if (timerLabel != null) {

                            timerLabel.setText("" + time);

                            timerLabel.setStyle(
                                    "-fx-font-size: 36px;"
                                    + "-fx-font-weight: bold;"
                                    + "-fx-text-fill: "
                                    + (time <= 5 ? C_RED : C_LIME)
                                    + ";"
                            );
                        }

                    });

                    if (time == 0 && !answered) {

                        answered = true;

                        out.println(
                                MyServer.CMD_ANSWER
                                + "NO_ANSWER:0"
                        );
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }

            }).start();
        } else if (msg.startsWith(MyServer.MSG_SCORES)) {
            String json = msg.substring(MyServer.MSG_SCORES.length());
            showMidRoundScores(json);

        } else if (msg.startsWith(MyServer.MSG_QUIZ_END)) {
            String json = msg.substring(MyServer.MSG_QUIZ_END.length());
            showFinalScoreboard(json);
        }
    }

    // ── Waiting Screen ────────────────────────────────────────────────────────
    private void showWaitingScreen() {
        Label head = bigLabel("LOBBY", C_LIME);
        Label name = smallLabel("Joined as:  " + studentName, "white");
        waitingCountLabel = smallLabel("Waiting for others...", C_SLATE);
        Label hint = smallLabel("Quiz will start when teacher clicks Start", C_SLATE);

        // Animated dots
        Label dots = bigLabel("•  •  •", C_LIME);
        new Thread(() -> {
            String[] frames = {"•        ", "•  •     ", "•  •  •  ", "   •  •  ", "      •  "};
            int i = 0;
            while (true) {
                final String f = frames[i++ % frames.length];
                Platform.runLater(() -> dots.setText(f));
                try {
                    Thread.sleep(350);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }).start();

        VBox root = centeredVBox(20, head, name, waitingCountLabel, dots, hint);
        setScene(root);
    }

    // ── Ready Screen ──────────────────────────────────────────────────────────
    private void showReadyScreen() {
        Label head = bigLabel("GET READY!", C_LIME);
        Label sub = smallLabel("First question coming up...", "white");
        VBox root = centeredVBox(20, head, sub);
        setScene(root);
    }

    // ── Question Screen ───────────────────────────────────────────────────────
    private void showQuestionScreen(Map<String, String> q) {
        // Timer
        timerRunning = false;
        timerLabel = new Label("--");
        timerLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + C_LIME + ";");

        // Progress
        String idxText = "Q " + q.getOrDefault("index", "?") + " / " + q.getOrDefault("total", "?");
        Label qIdx = smallLabel(idxText, C_SLATE);

        // Question text
        Label qText = new Label(q.getOrDefault("text", ""));
        qText.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
        qText.setWrapText(true);
        qText.setMaxWidth(750);
        qText.setAlignment(Pos.CENTER);

        // Top bar
        HBox topBar = new HBox(30, qIdx, timerLabel);
        topBar.setAlignment(Pos.CENTER);

        // Options grid
        String[] opts = {
            q.getOrDefault("optA", ""),
            q.getOrDefault("optB", ""),
            q.getOrDefault("optC", ""),
            q.getOrDefault("optD", "")
        };

        Button[] btns = new Button[4];
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < 4; i++) {
            final String letter = OPT_LETTERS[i];
            final String color = OPT_COLORS[i];
            final int idx = i;

            Button btn = new Button(letter + ".   " + opts[i]);
            btn.setMaxWidth(400);
            btn.setMinWidth(320);
            btn.setMinHeight(80);
            btn.setWrapText(true);
            btn.setStyle(
                    "-fx-background-color: " + C_DARK + "; -fx-text-fill: white; "
                    + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 14 22; "
                    + "-fx-background-radius: 12; -fx-border-color: " + color + "; "
                    + "-fx-border-width: 2; -fx-border-radius: 12; -fx-cursor: hand;"
            );
            btns[i] = btn;

            btn.setOnAction(e -> {
                if (answered) {
                    return;
                }
                answered = true;
                long elapsed = System.currentTimeMillis() - questionStartTime;
                out.println(MyServer.CMD_ANSWER + letter + ":" + elapsed);

                // Highlight selected, dim others
                for (int j = 0; j < 4; j++) {
                    btns[j].setDisable(true);
                    if (j != idx) {
                        btns[j].setOpacity(0.3);
                    }
                }
                btn.setOpacity(1.0);
                btn.setStyle(
                        "-fx-background-color: " + color + "; -fx-text-fill: white; "
                        + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 14 22; "
                        + "-fx-background-radius: 12; -fx-cursor: hand;"
                );

            });

            grid.add(btn, i % 2, i / 2);
        }

        VBox root = centeredVBox(25, topBar, qText, grid);
        root.setPadding(new Insets(40));
        setScene(root);
    }

    // ── Mid-round Scores ──────────────────────────────────────────────────────
    private void showMidRoundScores(String json) {
        List<String[]> entries = parseScoresJson(json);

        Label head = bigLabel("SCORES", C_LIME);
        Label waiting = smallLabel("Next question coming up...", C_SLATE);

        VBox list = new VBox(8);
        list.setAlignment(Pos.CENTER);
        String[] medals = {"1st", "2nd", "3rd"};

        for (int i = 0; i < entries.size(); i++) {
            String rank = (i < 3) ? medals[i] : ("#" + (i + 1));
            String name = entries.get(i)[0];
            String pts = entries.get(i)[1] + " pts";
            boolean isMe = name.equals(studentName);

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(440);
            row.setPadding(new Insets(10, 18, 10, 18));
            row.setStyle(
                    "-fx-background-color: " + (isMe ? "#1e3a5f" : C_DARK) + "; "
                    + "-fx-background-radius: 10;"
                    + (isMe ? " -fx-border-color: " + C_LIME + "; -fx-border-width: 2; -fx-border-radius: 10;" : "")
            );

            Label rl = smallLabel(rank, "white");
            rl.setMinWidth(45);
            rl.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

            Label nl = new Label(name + (isMe ? "  (you)" : ""));
            nl.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isMe ? C_LIME : "white")
                    + "; -fx-font-weight: bold;");
            HBox.setHgrow(nl, Priority.ALWAYS);

            Label pl = smallLabel(pts, C_SLATE);

            row.getChildren().addAll(rl, nl, pl);
            list.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(450);

        VBox root = centeredVBox(20, head, scroll, waiting);
        root.setPadding(new Insets(30));
        setScene(root);
    }

    // ── Final Scoreboard ──────────────────────────────────────────────────────
    private void showFinalScoreboard(String json) {
        List<String[]> entries = parseScoresJson(json);

        Label head = bigLabel("FINAL RESULTS", C_LIME);
        head.setPadding(new Insets(0, 0, 10, 0));

        // Find my rank
        int myRank = 1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i)[0].equals(studentName)) {
                myRank = i + 1;
                break;
            }
        }
        Label myRankLabel = smallLabel("Your rank:  #" + myRank, C_GREEN);
        myRankLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;");

        VBox list = new VBox(10);
        list.setAlignment(Pos.CENTER);
        String[] medals = {"1st", "2nd", "3rd"};

        for (int i = 0; i < entries.size(); i++) {
            String rank = (i < 3) ? medals[i] : ("#" + (i + 1));
            String name = entries.get(i)[0];
            String pts = entries.get(i)[1] + " pts";
            long timeMs = Long.parseLong(entries.get(i)[2]);
            boolean isMe = name.equals(studentName);
            boolean hasTie = i + 1 < entries.size()
                    && entries.get(i)[1].equals(entries.get(i + 1)[1]);

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(520);
            row.setPadding(new Insets(12, 20, 12, 20));
            row.setStyle(
                    "-fx-background-color: " + (isMe ? "#1e3a5f" : C_DARK) + "; "
                    + "-fx-background-radius: 10;"
                    + (isMe ? " -fx-border-color: " + C_LIME + "; -fx-border-width: 2; -fx-border-radius: 10;" : "")
                    + (hasTie && !isMe ? " -fx-border-color: #fbbf24; -fx-border-width: 1; -fx-border-radius: 10;" : "")
            );

            Label rl = new Label(rank);
            rl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (hasTie ? "#fbbf24" : "white") + ";");
            rl.setMinWidth(50);

            VBox nameBox = new VBox(3);
            Label nl = new Label(name + (isMe ? "  (you)" : "") + (hasTie ? "  [Tiebreak]" : ""));
            nl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                    + (isMe ? C_LIME : (hasTie ? "#fbbf24" : "white")) + ";");
            Label tl = new Label("Answer time: " + (timeMs / 1000.0) + "s");
            tl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_SLATE + ";");
            nameBox.getChildren().addAll(nl, tl);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            Label pl = new Label(pts);
            pl.setStyle("-fx-font-size: 16px; -fx-text-fill: " + C_SLATE + ";");

            row.getChildren().addAll(rl, nameBox, pl);
            list.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(450);

        VBox root = centeredVBox(20, head, myRankLabel, scroll);
        root.setPadding(new Insets(30));
        setScene(root);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private VBox centeredVBox(int spacing, javafx.scene.Node... nodes) {
        VBox box = new VBox(spacing);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label bigLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return l;
    }

    private Label smallLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 16px; -fx-text-fill: " + color + ";");
        return l;
    }

    private TextField inputField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(380);
        tf.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; "
                + "-fx-font-size: 16px; -fx-padding: 12; -fx-background-radius: 8;");
        return tf;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + C_LIME + "; -fx-text-fill: #1a1a1a; "
                + "-fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 45; "
                + "-fx-background-radius: 10; -fx-cursor: hand;");
        return b;
    }

    private List<String[]> parseScoresJson(String json) {
        List<String[]> result = new ArrayList<>();
        String[] entries = json.replaceAll("[\\[\\]{}]", "").split("(?<=\\}),(?=\\{)");
        for (String entry : entries) {
            String name = extractVal(entry, "name");
            String points = extractVal(entry, "points");
            String timeMs = extractVal(entry, "timeMs");
            if (name != null && points != null) {
                result.add(new String[]{name, points, timeMs != null ? timeMs : "0"});
            }
        }
        return result;
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String k : new String[]{"index", "total", "text", "optA", "optB", "optC", "optD"}) {
            String v = extractVal(json, k);
            if (v != null) {
                map.put(k, v);
            }
        }
        return map;
    }

    private String extractVal(String json, String key) {
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) {
            return null;
        }
        String rest = json.substring(idx + pat.length()).trim();
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

    @Override
    public void stop() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
