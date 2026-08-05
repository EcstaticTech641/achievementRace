package com.ronlab.achievementrace.session;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Manages active advancement pool generation and config blacklist filtering.
 */
@NullMarked
public class AdvancementPoolManager {

    private final Plugin plugin;
    private final Set<String> normalizedBlacklist;

    public AdvancementPoolManager(Plugin plugin, Set<String> blacklist) {
        this.plugin = plugin;
        this.normalizedBlacklist = new HashSet<>();
        for (String entry : blacklist) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim().toLowerCase();
                if (!trimmed.contains(":")) {
                    trimmed = "minecraft:" + trimmed;
                }
                this.normalizedBlacklist.add(trimmed);
            }
        }
    }

    /**
     * Generates a new pool of active advancements, excluding recipes and blacklisted keys.
     */
    public List<Advancement> buildActivePool() {
        List<Advancement> activePool = new ArrayList<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        int blacklistedCount = 0;

        while (iterator.hasNext()) {
            Advancement adv = iterator.next();
            NamespacedKey key = adv.getKey();

            // Filter recipe advancements
            if (key.getKey().startsWith("recipes/")) {
                continue;
            }

            // Filter blacklisted keys (handling full minecraft: namespaces)
            String fullKey = key.toString().toLowerCase();
            if (normalizedBlacklist.contains(fullKey)) {
                blacklistedCount++;
                continue;
            }

            activePool.add(adv);
        }

        plugin.getLogger().info("Filtered " + blacklistedCount + " advancements via config.yml blacklist.");
        plugin.getLogger().info("Generated active advancement pool with " + activePool.size() + " advancements.");
        return activePool;
    }
}
