package client;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
public class App extends Application {
    
    @Override
    public void start(Stage primaryStage)
    {
        Label title=new Label("HYPERSYNC");
        title.setStyle("-fx-font-size: 70px; -fx-font-weight: bold; -fx-text-fill: #deff9a; -fx-letter-spacing: 8px;");
        Label subtitle = new Label("High-Performance Real-time Quiz");
        subtitle.setStyle("-fx-font-size: 20px; -fx-text-fill: #ffffff; -fx-opacity: 0.7; -fx-padding: 0 0 50 0;");

        Button btnCreate = new Button("Create Game");
        Button btnJoin = new Button("Join Game");
        String primaryBtnStyle = "-fx-background-color: #deff9a; -fx-text-fill: #1a1a1a; -fx-font-size: 22px; " +"-fx-font-weight: bold; -fx-padding: 15 45; -fx-background-radius: 10; -fx-cursor: hand;";
        
        String secondaryBtnStyle = "-fx-background-color: transparent; -fx-text-fill: #deff9a; -fx-font-size: 22px; " +"-fx-font-weight: bold; -fx-padding: 15 45; -fx-border-color: #deff9a; " +"-fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;";
        btnCreate.setStyle(primaryBtnStyle);
        btnJoin.setStyle(secondaryBtnStyle);
        HBox container = new HBox(30);
        container.setAlignment(Pos.CENTER);
        container.getChildren().addAll(btnCreate, btnJoin);
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a;"); 
        root.getChildren().addAll(title, subtitle, container);
        btnCreate.setOnAction(e -> {
            
            showTeacherDashboard(primaryStage);
        });
        btnJoin.setOnAction(e -> {
            
            showJoinPopup(primaryStage);
        });
        Scene scene = new Scene(root, 1000, 720);
        primaryStage.setTitle("HyperSync v1.0");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(false);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
    private void showTeacherDashboard(Stage primaryStage) {
    VBox layout = new VBox(30);
    layout.setAlignment(Pos.CENTER);
    layout.setStyle("-fx-background-color: #0f172a;"); 

    Label head = new Label("SELECT GAME MODE");
    head.setStyle("-fx-text-fill: #deff9a; -fx-font-size: 35px; -fx-font-weight: bold; -fx-padding: 0 0 20 0;");

    
    Button btnMarathon = new Button("MARATHON MODE");
    Label descMarathon = new Label("Focus on accuracy. More time per question.");
    descMarathon.setStyle("-fx-text-fill: white; -fx-opacity: 0.6;");


    Button btnRapid = new Button("RAPID FIRE MODE");
    Label descRapid = new Label("Focus on speed. Very limited time (30-60 sec).");
    descRapid.setStyle("-fx-text-fill: white; -fx-opacity: 0.6;");

    
    String modeBtnStyle = "-fx-background-color: #1e293b; -fx-text-fill: #deff9a; -fx-font-size: 24px; " +"-fx-font-weight: bold; -fx-padding: 20 60; -fx-border-color: #deff9a; " +"-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";

    btnMarathon.setStyle(modeBtnStyle);
    btnRapid.setStyle(modeBtnStyle);

    
    btnMarathon.setOnMouseEntered(e -> btnMarathon.setStyle(modeBtnStyle + "-fx-background-color: #334155;"));
    btnMarathon.setOnMouseExited(e -> btnMarathon.setStyle(modeBtnStyle));
    
    btnRapid.setOnMouseEntered(e -> btnRapid.setStyle(modeBtnStyle + "-fx-background-color: #334155;"));
    btnRapid.setOnMouseExited(e -> btnRapid.setStyle(modeBtnStyle));

    btnMarathon.setOnAction(e -> {
    showModeSettings(primaryStage, "Marathon");
    });
    btnRapid.setOnAction(e -> {
    showModeSettings(primaryStage, "Rapid Fire");
    });
    VBox marathonBox = new VBox(10, btnMarathon, descMarathon);
    marathonBox.setAlignment(Pos.CENTER);
    
    VBox rapidBox = new VBox(10, btnRapid, descRapid);
    rapidBox.setAlignment(Pos.CENTER);

    HBox modesContainer = new HBox(50);
    modesContainer.setAlignment(Pos.CENTER);
    modesContainer.getChildren().addAll(marathonBox, rapidBox);

    Button btnBack = new Button("Back to Main Menu");
    btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-underline: true; -fx-cursor: hand;");
    btnBack.setOnAction(e -> start(primaryStage)); // પાછા જવા માટે

    layout.getChildren().addAll(head, modesContainer, btnBack);

    Scene scene = new Scene(layout, 1000, 720);
    primaryStage.setScene(scene);
    primaryStage.setMaximized(false);
    primaryStage.setMaximized(true);
    primaryStage.show();
}
private void showModeSettings(Stage primaryStage, String mode) {
    VBox layout = new VBox(25);
    layout.setAlignment(Pos.CENTER);
    layout.setStyle("-fx-background-color: #0f172a;");

    Label head = new Label(mode.toUpperCase() + " CONFIGURATION");
    head.setStyle("-fx-text-fill: #deff9a; -fx-font-size: 32px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

    // --- Inputs ---
    TextField queCountField = new TextField();
    queCountField.setPromptText("Total Number of Questions");
    queCountField.setMaxWidth(350);
    queCountField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 12; -fx-background-radius: 8;");

    TextField timeField = new TextField();
    if (mode.equalsIgnoreCase("Marathon")) {
        timeField.setPromptText("Total Quiz Time (in minutes)");
    } else {
        timeField.setPromptText("Time per Question (in seconds)");
    }
    timeField.setMaxWidth(350);
    timeField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 12; -fx-background-radius: 8;");

    // --- Action Buttons Section ---
    Button btnnext = new Button("NEXT");
    btnnext.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 60; -fx-background-radius: 10; -fx-cursor: hand;");

    Button btnBack = new Button("Back to Mode Selection");
    btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand; -fx-underline: true;");
    
    // Actions
    btnnext.setOnAction(e -> {
        try {
            int qCount = Integer.parseInt(queCountField.getText());
            int tVal = Integer.parseInt(timeField.getText());
            int finalTime = mode.equalsIgnoreCase("Marathon") ? tVal * 60 : tVal;
            showQuestionEntry(primaryStage, mode, qCount, finalTime);
        } catch (Exception ex) {
            System.out.println("Invalid Input!");
        }
    });

    btnBack.setOnAction(e -> showTeacherDashboard(primaryStage));

    // Focus Fix: આ લેબલ પર ફોકસ જશે એટલે TextField ખાલી દેખાશે
    Label dummyFocus = new Label();
    dummyFocus.setFocusTraversable(true);

    // બધા એલિમેન્ટ્સને VBox માં ઉમેરવા
    layout.getChildren().addAll(dummyFocus, head, queCountField, timeField, btnnext, btnBack);
    
    // મેથડના અંતે ફોકસ ટ્રાન્સફર
    dummyFocus.requestFocus();

    Scene scene = new Scene(layout, 1000, 720);
    primaryStage.setScene(scene);
    
    if (primaryStage.isMaximized()) {
        primaryStage.setMaximized(false);
        primaryStage.setMaximized(true);
    }
}
private void showWaitingLobby(Stage primaryStage, String mode, int totalQue, int timeVal) {
    
    VBox layout = new VBox(25);
    layout.setAlignment(Pos.CENTER);
    layout.setStyle("-fx-background-color: #0f172a;");

    Label modeLabel = new Label("MODE: " + mode.toUpperCase());
    modeLabel.setStyle("-fx-text-fill: #deff9a; -fx-font-size: 20px;");

    Label statusLabel = new Label("WAITING FOR STUDENTS...");
    statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 35px; -fx-font-weight: bold;");

    // આ લેબલ આપણે પાછળથી સર્વર સાથે કનેક્ટ કરીશું
    Label countLabel = new Label("0 Students Joined");
    countLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 24px;");

    // સેટિંગ્સનું લિસ્ટ બતાવવા માટે
    Label settingsLabel = new Label(totalQue + " Questions | " + (mode.equalsIgnoreCase("Marathon") ? (timeVal/60 + " min") : (timeVal + " sec/que")));
    settingsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");

    Button btnStartQuiz = new Button("START QUIZ NOW");
    btnStartQuiz.setStyle("-fx-background-color: #deff9a; -fx-text-fill: #1a1a1a; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 50; -fx-cursor: hand;");
    
    // અત્યારે બટન ચાલુ રાખીએ છીએ ટેસ્ટિંગ માટે
    btnStartQuiz.setOnAction(e -> {
        System.out.println("Starting Quiz with " + totalQue + " questions...");
    });

    layout.getChildren().addAll(modeLabel, statusLabel, countLabel, settingsLabel, btnStartQuiz);

    Scene scene = new Scene(layout, 1000, 720);
    primaryStage.setScene(scene);

    // ફૂલ સ્ક્રીન જાળવી રાખવા માટેનો આપણો ટ્રીક
    if (primaryStage.isMaximized()) {
        primaryStage.setMaximized(false);
        primaryStage.setMaximized(true);
    }
}
private void showQuestionEntry(Stage primaryStage, String mode, int totalQue, int timeVal) {
    VBox layout = new VBox(15);
    layout.setAlignment(Pos.CENTER);
    layout.setPadding(new Insets(30));
    layout.setStyle("-fx-background-color: #0f172a;");

    // પ્રશ્નનો નંબર ટ્રેક કરવા માટે
    final int[] currentQueNum = {1}; 
    
    Label head = new Label("QUESTION " + currentQueNum[0] + " OF " + totalQue);
    head.setStyle("-fx-text-fill: #deff9a; -fx-font-size: 28px; -fx-font-weight: bold;");

    TextArea queArea = new TextArea();
    queArea.setPromptText("Enter your question here...");
    queArea.setMaxSize(600, 100);
    queArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-font-size: 16px;");

    // ઓપ્શન્સ
    TextField opt1 = new TextField(); opt1.setPromptText("Option A");
    TextField opt2 = new TextField(); opt2.setPromptText("Option B");
    TextField opt3 = new TextField(); opt3.setPromptText("Option C");
    TextField opt4 = new TextField(); opt4.setPromptText("Option D");

    String optStyle = "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;";
    opt1.setStyle(optStyle); opt2.setStyle(optStyle); opt3.setStyle(optStyle); opt4.setStyle(optStyle);
    opt1.setMaxWidth(400); opt2.setMaxWidth(400); opt3.setMaxWidth(400); opt4.setMaxWidth(400);

    ChoiceBox<String> correctAns = new ChoiceBox<>();
    correctAns.getItems().addAll("A", "B", "C", "D");
    correctAns.setValue("A");

    // અહીં રહ્યું તારું બટન - નામ રાખ્યું છે btnSubmitNext
    Button btnSubmitNext = new Button("NEXT QUESTION ➜");
    btnSubmitNext.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12 40; -fx-cursor: hand;");

    btnSubmitNext.setOnAction(e -> {
        // ડેટા સ્ટોર કરવાનું લોજિક
        String fullQuestion = queArea.getText() + "|" + opt1.getText() + "|" + opt2.getText() + "|" + opt3.getText() + "|" + opt4.getText() + "|" + correctAns.getValue();
        // questionsList.add(fullQuestion); // આ રીતે લિસ્ટમાં ઉમેરી શકાય

        if (currentQueNum[0] < totalQue) {
            currentQueNum[0]++;
            head.setText("QUESTION " + currentQueNum[0] + " OF " + totalQue);
            
            // ફિલ્ડ્સ ક્લિયર કરવા
            queArea.clear(); opt1.clear(); opt2.clear(); opt3.clear(); opt4.clear();
            
            // જો છેલ્લો પ્રશ્ન હોય તો બટનનું લખાણ બદલી નાખવું
            if(currentQueNum[0] == totalQue) {
                btnSubmitNext.setText("FINISH & LAUNCH 🚀");
            }
        } else {
            // બધા પ્રશ્નો પતી ગયા, હવે લોબીમાં જાવ
            showWaitingLobby(primaryStage, mode, totalQue, timeVal);
        }
    });

    layout.getChildren().addAll(head, queArea, opt1, opt2, opt3, opt4, new Label("Select Correct Option:"), correctAns, btnSubmitNext);

    Scene scene = new Scene(layout, 1000, 720);
    primaryStage.setScene(scene);

    if (primaryStage.isMaximized()) {
        primaryStage.setMaximized(false);
        primaryStage.setMaximized(true);
    }
}
private void showJoinPopup(Stage owner){
        System.out.println("Opening Join Popup...");
}
public static void main(String[] args) {
        launch(args);
    }
}
