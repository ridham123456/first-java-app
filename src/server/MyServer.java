package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class MyServer {

    // ── Protocol constants ──────────────────────────────────────────────────
    // Server → Client messages
    public static final String MSG_JOINED        = "JOINED:";          // JOINED:<name>
    public static final String MSG_STUDENT_LIST  = "STUDENT_LIST:";    // STUDENT_LIST:A,B,C
    public static final String MSG_QUESTION      = "QUESTION:";        // QUESTION:<json>
    public static final String MSG_ANSWER_RESULT = "ANSWER_RESULT:";   // ANSWER_RESULT:CORRECT|WRONG|<correct>
    public static final String MSG_SCORES        = "SCORES:";          // SCORES:<json>
    public static final String MSG_QUIZ_END      = "QUIZ_END:";        // QUIZ_END:<scoreJson>
    public static final String MSG_START         = "START";
    public static final String MSG_TIMER         = "TIMER:";           // TIMER:<seconds>
    public static final String MSG_ERROR         = "ERROR:";

    // Client → Server messages
    public static final String CMD_JOIN          = "JOIN:";            // JOIN:<name>
    public static final String CMD_ANSWER        = "ANSWER:";          // ANSWER:<A|B|C|D>:<timestamp>
    public static final String CMD_START         = "START_QUIZ";

    // ───────────────────────────────────────────────────────────────────────

    private static final int PORT = 5555;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final LobbyManager lobbyManager = new LobbyManager();
    private final ScoreTracker scoreTracker = new ScoreTracker();
    private QuizEngine quizEngine;

    private volatile boolean quizRunning = false;

    public static MyServer INSTANCE;

    public static void main(String[] args) {
        INSTANCE = new MyServer();
        INSTANCE.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("╔══════════════════════════════╗");
            System.out.println("║  HyperSync Server Started    ║");
            System.out.println("║  IP   : " + padRight(ip, 22) + "║");
            System.out.println("║  Port : " + padRight(String.valueOf(PORT), 22) + "║");
            System.out.println("╚══════════════════════════════╝");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Called by teacher to configure and start quiz
    public void setupQuiz(String mode, int totalQuestions, int timeValue, List<QuizEngine.Question> questions) {
        quizEngine = new QuizEngine(mode, totalQuestions, timeValue, questions);
        scoreTracker.init(lobbyManager.getStudentNames());
    }

    public void startQuiz() {
        if (quizEngine == null) {
        System.out.println("Quiz not configured.");
        return;
    }

    if (lobbyManager.getCount() == 0) {
        System.out.println("No students joined.");
        return;
    }
        quizRunning = true;
        broadcast(MSG_START);
        new Thread(this::runQuizLoop).start();
    }

    private void runQuizLoop() {
        while (quizEngine.hasNextQuestion()) {
            QuizEngine.Question q = quizEngine.nextQuestion();
            scoreTracker.resetRoundAnswers();

            // Build question JSON manually (no external libs)
            String qJson = buildQuestionJson(q, quizEngine.getCurrentIndex(), quizEngine.getTotalQuestions());
            broadcast(MSG_QUESTION + qJson);

            // Timer countdown
            int timeLeft = quizEngine.getTimePerQuestion();
            for (int t = timeLeft; t >= 0; t--) {
                broadcast(MSG_TIMER + t);

            if (scoreTracker.allAnswered(lobbyManager.getCount())) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            }

            // Reveal correct answer & broadcast scores
            String correct = q.correctOption;
            scoreTracker.finalizeRound(correct, quizEngine.getMode());

            String scoresJson = scoreTracker.getScoresJson();
            broadcast(MSG_SCORES + scoresJson);

            // Short pause between questions
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        }

        // Quiz finished
        quizRunning = false;
        String finalJson = scoreTracker.getFinalScoreboardJson();
        broadcast(MSG_QUIZ_END + finalJson);
    }

    // Called from ClientHandler when a student submits an answer
    public synchronized void receiveAnswer(String studentName, String option, long timestamp) {
        if (!quizRunning) return;
        scoreTracker.recordAnswer(studentName, option, timestamp);
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.send(message);
            }
        }
    }

    public void sendToTeacher(String message) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                if (ch.isTeacher()) {
                    ch.send(message);
                    break;
                }
            }
        }
    }

    public LobbyManager getLobbyManager() { return lobbyManager; }
    public ScoreTracker getScoreTracker() { return scoreTracker; }
    public boolean isQuizRunning() { return quizRunning; }
    public List<ClientHandler> getClients() { return clients; }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        if (!handler.isTeacher() && handler.getName() != null) {
            lobbyManager.removeStudent(handler.getName());
            broadcastStudentList();
        }
    }

    public void broadcastStudentList() {
        String names = String.join(",", lobbyManager.getStudentNames());
        broadcast(MSG_STUDENT_LIST + names);
    }

    private String buildQuestionJson(QuizEngine.Question q, int index, int total) {
        return "{\"index\":" + index +
               ",\"total\":" + total +
               ",\"text\":\"" + escape(q.text) + "\"" +
               ",\"optA\":\"" + escape(q.optA) + "\"" +
               ",\"optB\":\"" + escape(q.optB) + "\"" +
               ",\"optC\":\"" + escape(q.optC) + "\"" +
               ",\"optD\":\"" + escape(q.optD) + "\"" +
               "}";
    }

    private String escape(String s) {
    return s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "");
}

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}