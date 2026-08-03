package com.ronlab.achievementrace.session;

import com.ronlab.achievementrace.AchievementRacePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active AchievementRaceSession instances bound to session world names.
 */
@NullMarked
public class SessionManager {

    private final AchievementRacePlugin plugin;
    private final Map<String, AchievementRaceSession> activeSessions = new ConcurrentHashMap<>();

    public SessionManager(AchievementRacePlugin plugin) {
        this.plugin = plugin;
    }

    public @Nullable AchievementRaceSession createSession(String worldName, List<UUID> playerUuids) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Cannot start AchievementRaceSession: World '" + worldName + "' not loaded!");
            return null;
        }

        AchievementRaceSession session = new AchievementRaceSession(plugin, world, playerUuids);
        activeSessions.put(worldName, session);
        session.start();
        plugin.getLogger().info("Started AchievementRaceSession for world '" + worldName + "' with " + playerUuids.size() + " players.");
        return session;
    }

    public @Nullable AchievementRaceSession getSession(String worldName) {
        return activeSessions.get(worldName);
    }

    public @Nullable AchievementRaceSession findSessionByWorld(World world) {
        return activeSessions.get(world.getName());
    }

    public void removeSession(String worldName) {
        AchievementRaceSession session = activeSessions.remove(worldName);
        if (session != null) {
            session.stop();
            plugin.getLogger().info("Cleaned up session for world '" + worldName + "'.");
        }
    }

    public void terminateAllSessions() {
        for (String worldName : List.copyOf(activeSessions.keySet())) {
            removeSession(worldName);
        }
    }

    public Map<String, AchievementRaceSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }
}
