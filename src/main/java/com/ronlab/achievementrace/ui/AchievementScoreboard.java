package com.ronlab.achievementrace.ui;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Manages dynamic sidebar scoreboard display for session players.
 */
@NullMarked
public class AchievementScoreboard {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<UUID, Integer> scores;
    private final int targetScore;

    public AchievementScoreboard(int targetScore, Map<UUID, Integer> initialScores) {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.targetScore = targetScore;
        this.scores = new HashMap<>(initialScores);

        Component title = Component.text("ACHIEVEMENT RACE", NamedTextColor.GOLD, TextDecoration.BOLD);
        this.objective = scoreboard.registerNewObjective("ach_race", Criteria.DUMMY, title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.numberFormat(NumberFormat.blank());
    }

    public void update(String currentObjectiveName, int remainingSeconds, Collection<Player> players) {
        // Clear previous scores
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int scoreIndex = 15;

        objective.getScore("§7--------------------").setScore(scoreIndex--);

        // Target Objective
        objective.getScore("§eTarget: §f" + truncate(currentObjectiveName, 24)).setScore(scoreIndex--);
        objective.getScore("§eGoal: §f" + targetScore + " pts").setScore(scoreIndex--);

        objective.getScore("§7 ").setScore(scoreIndex--);

        // Player Scores
        List<Map.Entry<UUID, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (Map.Entry<UUID, Integer> entry : sortedScores) {
            if (scoreIndex <= 3) break;
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = (p != null) ? p.getName() : entry.getKey().toString().substring(0, 8);
            objective.getScore("§a" + truncate(name, 16) + ": §f" + entry.getValue()).setScore(scoreIndex--);
        }

        objective.getScore("§7  ").setScore(scoreIndex--);

        // Time Remaining
        if (remainingSeconds >= 0) {
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            String timeStr = String.format("%d:%02d", minutes, seconds);
            objective.getScore("§eTime: §f" + timeStr).setScore(scoreIndex--);
        }

        objective.getScore("§7-------------------").setScore(scoreIndex--);

        // Apply to online players
        for (Player player : players) {
            if (player.isOnline()) {
                player.setScoreboard(scoreboard);
            }
        }
    }

    public void updatePlayerScore(UUID uuid, int score) {
        scores.put(uuid, score);
    }

    public void resetPlayers(Collection<Player> players) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : players) {
            if (player.isOnline()) {
                player.setScoreboard(main);
            }
        }
        try {
            objective.unregister();
        } catch (IllegalStateException ignored) {
            // Objective already unregistered
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }
}
