package com.ronlab.achievementrace.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates settings loaded from settings.yml.
 */
@NullMarked
public class Settings {

    public enum GameMode {
        RACE,
        HUNT
    }

    private final GameMode mode;
    private final int targetScore;
    private final int matchDurationSeconds;
    private final Set<String> blacklist;

    public Settings(FileConfiguration config) {
        String modeStr = config.getString("mode", "RACE").toUpperCase();
        GameMode parsedMode;
        try {
            parsedMode = GameMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            parsedMode = GameMode.RACE;
        }
        this.mode = parsedMode;
        this.targetScore = config.getInt("target-score", 5);
        this.matchDurationSeconds = config.getInt("match-duration-seconds", 300);

        List<String> rawBlacklist = config.getStringList("blacklist");
        this.blacklist = new HashSet<>();
        for (String key : rawBlacklist) {
            if (key != null && !key.isBlank()) {
                this.blacklist.add(key.trim().toLowerCase());
            }
        }
    }

    public GameMode getMode() {
        return mode;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public int getMatchDurationSeconds() {
        return matchDurationSeconds;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }
}
