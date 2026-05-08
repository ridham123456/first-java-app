package server;

import java.util.*;

public class QuizEngine {

    public static class Question {
        public final String text;
        public final String optA, optB, optC, optD;
        public final String correctOption; // "A", "B", "C", or "D"

        public Question(String text, String optA, String optB, String optC, String optD, String correct) {
            this.text          = text;
            this.optA          = optA;
            this.optB          = optB;
            this.optC          = optC;
            this.optD          = optD;
            this.correctOption = correct;
        }

        // Parse from pipe-delimited string: text|A|B|C|D|correct
        public static Question fromString(String s) {
            String[] p = s.split("\\|", -1);
            if (p.length < 6) throw new IllegalArgumentException("Bad question format: " + s);
            return new Question(p[0], p[1], p[2], p[3], p[4], p[5]);
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    private final String mode;            // "Marathon" or "Rapid Fire"
    private final int totalQuestions;
    private final int timeValue;          // Marathon = total seconds, Rapid = sec/question
    private final List<Question> questions;
    private int currentIndex = 0;

    public QuizEngine(String mode, int totalQuestions, int timeValue, List<Question> questions) {
        this.mode           = mode;
        this.totalQuestions = totalQuestions;
        this.timeValue      = timeValue;
        this.questions      = new ArrayList<>(questions);
    }

    public boolean hasNextQuestion() {
        return currentIndex < questions.size();
    }

    public Question nextQuestion() {
        return questions.get(currentIndex++);
    }

    public int getCurrentIndex()    { return currentIndex; }
    public int getTotalQuestions()  { return totalQuestions; }
    public String getMode()         { return mode; }

    /**
     * Returns per-question time in seconds.
     * Marathon: divide total time equally.
     * Rapid Fire: fixed timeValue per question.
     */
    public List<Question> getQuestions() {
    return questions;
    }
    public int getTimePerQuestion() {
        if (mode.equalsIgnoreCase("Marathon")) {
            return totalQuestions > 0 ? timeValue / totalQuestions : 60;
        } else {
            return timeValue; // already in seconds
        }
    }
}