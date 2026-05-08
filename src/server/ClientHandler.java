package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final MyServer server;
    private PrintWriter out;
    private BufferedReader in;

    private String name;
    private boolean teacher = false;
    private int currentQuestionIndex = 0;
    private QuizEngine.Question currentQuestion;
    private boolean answeredCurrent = false;

    public ClientHandler(Socket socket, MyServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            // client disconnected
        } finally {
            server.removeClient(this);
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleMessage(String msg) {
        if (msg.startsWith(MyServer.CMD_JOIN)) {
            // JOIN:<name>  or  JOIN:TEACHER:<name>
            String payload = msg.substring(MyServer.CMD_JOIN.length());
            if (payload.startsWith("TEACHER:")) {
                teacher = true;
                name = payload.substring("TEACHER:".length());
                System.out.println("[Server] Teacher connected: " + name);
            } else {
                String tempName = payload;

                if (server.isQuizRunning()) {
                    send(MyServer.MSG_ERROR + "Quiz already started");
                    return;
                }

                if (server.getLobbyManager().contains(tempName)) {
                    send(MyServer.MSG_ERROR + "Name already taken");

                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }

                    return;
                }

                name = tempName;
                server.getLobbyManager().addStudent(name);
                System.out.println("[Server] Student joined: " + name);
                send(MyServer.MSG_JOINED + name);
                server.broadcastStudentList();
            }

        } else if (msg.startsWith(MyServer.CMD_ANSWER)) {
            // ANSWER:<option>:<timestamp>
            String[] parts = msg.substring(MyServer.CMD_ANSWER.length()).split(":");
            if (parts.length >= 2) {
                String option = parts[0];
                long timestamp;

                try {
                    timestamp = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    return;
                }
                if (answeredCurrent
                        && !option.equals("NO_ANSWER")) {

                    return;
                }

                answeredCurrent = true;

                server.receiveAnswer(name, option, timestamp);
                if (option.equalsIgnoreCase(
                        currentQuestion.correctOption)) {

                    ScoreTracker.PlayerScore ps
                            = server.getScoreTracker()
                                    .getScores()
                                    .get(name);

                    if (ps != null) {
                        ps.points += 10;
                    }
                }

                sendNextQuestion();
            }

        } else if (msg.equals(MyServer.CMD_START)) {
            if (teacher) {
                server.startQuiz();
            }
        }
    }

    public void sendNextQuestion() {

        QuizEngine engine = server.getQuizEngine();

        if (engine == null) {
            return;
        }

        if (currentQuestionIndex >= engine.getQuestions().size()) {

            server.studentFinished();
            return;
        }

        System.out.println(
                "Sending question "
                + currentQuestionIndex
                + " to " + name
        );

        QuizEngine.Question q
                = engine.getQuestions().get(currentQuestionIndex);
        currentQuestion = q;
        answeredCurrent = false;

        String qJson
                = server.buildQuestionJson(
                        q,
                        currentQuestionIndex + 1,
                        engine.getTotalQuestions()
                );

        send(MyServer.MSG_QUESTION + qJson);
        send(MyServer.MSG_TIMER
                + engine.getTimePerQuestion());

        currentQuestionIndex++;
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isTeacher() {
        return teacher;
    }
}
