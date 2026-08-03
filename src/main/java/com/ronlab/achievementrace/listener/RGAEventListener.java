package com.ronlab.achievementrace.listener;

import com.ronlab.achievementrace.AchievementRacePlugin;
import com.ronlab.achievementrace.session.AchievementRaceSession;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.UUID;

/**
 * Event listener for RGA minigame lifecycle transitions.
 */
@NullMarked
public class RGAEventListener implements Listener {

    private final AchievementRacePlugin plugin;

    public RGAEventListener(AchievementRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMinigameStart(MinigameStartEvent event) {
        String eventGameId = event.getMinigameId();
        if (!isTargetGame(eventGameId)) {
            return;
        }

        plugin.getLogger().info("Received MinigameStartEvent for " + eventGameId + " in world: " + event.getWorldName());
        plugin.getSessionManager().createSession(event.getWorldName(), event.getPlayerUuids());
    }

    @EventHandler
    public void onMinigameConclude(MinigameConcludeEvent event) {
        String eventGameId = event.getMinigameId();
        if (!isTargetGame(eventGameId)) {
            return;
        }

        plugin.getLogger().info("Received MinigameConcludeEvent for " + eventGameId + " in world: " + event.getWorldName());

        AchievementRaceSession session = plugin.getSessionManager().getSession(event.getWorldName());
        if (session != null) {
            // Populate scores into event.getScores()
            Map<UUID, Integer> sessionScores = session.getPlayerScores();
            for (Map.Entry<UUID, Integer> entry : sessionScores.entrySet()) {
                event.getScores().put(entry.getKey(), entry.getValue());
            }

            // Remove and stop session
            plugin.getSessionManager().removeSession(event.getWorldName());
        }
    }

    private boolean isTargetGame(String minigameId) {
        String gameIdFull = AchievementRacePlugin.GAME_ID.asString();
        String gameIdKey = AchievementRacePlugin.GAME_ID.key();
        return minigameId.equalsIgnoreCase(gameIdFull) || minigameId.equalsIgnoreCase(gameIdKey);
    }
}
