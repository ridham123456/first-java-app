package server;

import java.util.*;

public class ScoreTracker {

    // Points config
    private static final int POINTS_CORRECT_MARATHON   = 10;
    private static final int POINTS_CORRECT_RAPID      = 15;
    private static final int POINTS_SPEED_BONUS_MAX    = 5;  // extra points for fast answer in rapid fire

    public static class PlayerScore {
        public String name;
        public int    points      = 0;
        public long   totalTimeMs = 0;  // sum of answer timestamps (ms from question shown) — lower = faster
        public int    streak      = 0;  // consecutive correct answers

        public PlayerScore(String name) { this.name = name; }
    }

    public static class RoundAnswer {
        public String option;
        public long   timestamp;  // ms since question was displayed (client-side)
        public RoundAnswer(String o, long t) { option = o; timestamp = t; }
    }

    // ────────────────────────────────────────────────────────────────────────

    private final Map<String, PlayerScore>  scores  = new LinkedHashMap<>();
    private final Map<String, RoundAnswer> answers =Collections.synchronizedMap(new LinkedHashMap<>());

    public void init(List<String> names) {
        scores.clear();
        for (String n : names) scores.put(n, new PlayerScore(n));
    }

    public void resetRoundAnswers() {
        answers.clear();
    }

    public synchronized void recordAnswer(String name, String option, long timestamp) {
        if (!answers.containsKey(name)) { // first answer only
            answers.put(name, new RoundAnswer(option, timestamp));
        }
    }

    public boolean allAnswered(int expectedCount) {
        return answers.size() >= expectedCount;
    }

    /**
     * Award points after a question closes.
     * Tiebreak: stored as totalTimeMs — used only at the final scoreboard.
     */
    public void finalizeRound(String correctOption, String mode) {
        for (Map.Entry<String, RoundAnswer> e : answers.entrySet()) {
            String     name   = e.getKey();
            RoundAnswer ans   = e.getValue();
            PlayerScore ps    = scores.get(name);
            if (ps == null) continue;

            ps.totalTimeMs += ans.timestamp;

            if (ans.option.equalsIgnoreCase(correctOption)) {
                boolean isRapid = mode.equalsIgnoreCase("Rapid Fire");
                int base = isRapid ? POINTS_CORRECT_RAPID : POINTS_CORRECT_MARATHON;

                // Speed bonus for Rapid Fire (max 5 pts for answering within 5 sec)
                int bonus = 0;
                if (isRapid && ans.timestamp < 5000) {
                    bonus = (int) Math.max(0, POINTS_SPEED_BONUS_MAX - (ans.timestamp / 1000));
                }

                ps.points += base + bonus;
                ps.streak++;
            } else {
                ps.streak = 0;
            }
        }

        // Players who didn't answer at all — streak broken
        for (Map.Entry<String, PlayerScore> e : scores.entrySet()) {
            if (!answers.containsKey(e.getKey())) {
                e.getValue().streak = 0;
            }
        }
    }

    // ── JSON builders ────────────────────────────────────────────────────────

    /** Mid-game scores (sorted by points desc, tiebreak by time asc) */
    public String getScoresJson() {
        return buildJson(getSortedScores(), false);
    }

    /** Final scoreboard with tiebreak info */
    public String getFinalScoreboardJson() {
        return buildJson(getSortedScores(), true);
    }

    private List<PlayerScore> getSortedScores() {
        List<PlayerScore> list = new ArrayList<>(scores.values());
        list.sort((a, b) -> {
            if (b.points != a.points) return b.points - a.points;
            return Long.compare(a.totalTimeMs, b.totalTimeMs); // faster = lower = better
        });
        return list;
    }

    private String buildJson(List<PlayerScore> list, boolean includeTiebreak) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            PlayerScore ps = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"rank\":").append(i + 1)
              .append(",\"name\":\"").append(escape(ps.name)).append("\"")
              .append(",\"points\":").append(ps.points)
              .append(",\"streak\":").append(ps.streak);
            if (includeTiebreak) {
                sb.append(",\"timeMs\":").append(ps.totalTimeMs);
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public Map<String, PlayerScore> getScores() { return scores; }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}