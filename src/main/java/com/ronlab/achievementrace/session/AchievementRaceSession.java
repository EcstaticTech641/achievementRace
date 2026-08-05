package com.ronlab.achievementrace.session;

import com.ronlab.achievementrace.AchievementRacePlugin;
import com.ronlab.achievementrace.config.Settings;
import com.ronlab.achievementrace.ui.AchievementScoreboard;
import com.ronlab.rga.api.RGASessionControl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages an active Achievement Race minigame session bound to a single world instance.
 */
@NullMarked
public class AchievementRaceSession implements Listener {

    private final AchievementRacePlugin plugin;
    private final World world;
    private final List<UUID> playerUuids;
    private final int initialPlayerCount;
    private final Map<UUID, Integer> playerScores = new ConcurrentHashMap<>();
    private final Set<NamespacedKey> claimedAdvancements = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Advancement> activeAdvancementPool = new ArrayList<>();

    private final Settings.GameMode gameMode;
    private final int targetScore;
    private final int matchDurationSeconds;

    private final AchievementPromptManager promptManager;
    private final AchievementScoreboard scoreboard;

    private @Nullable BukkitTask timerTask;
    private @Nullable BukkitTask hudTask;
    private int remainingSeconds;
    private boolean active = false;

    public AchievementRaceSession(AchievementRacePlugin plugin, World world, List<UUID> playerUuids) {
        this.plugin = plugin;
        this.world = world;
        this.playerUuids = List.copyOf(playerUuids);
        this.initialPlayerCount = playerUuids.size();

        Settings settings = plugin.getSettings();
        this.gameMode = settings.getMode();
        this.targetScore = settings.getTargetScore();
        this.matchDurationSeconds = settings.getMatchDurationSeconds();
        this.remainingSeconds = matchDurationSeconds;

        for (UUID uuid : playerUuids) {
            playerScores.put(uuid, 0);
        }

        AdvancementPoolManager poolManager = new AdvancementPoolManager(plugin, settings.getBlacklist());
        this.activeAdvancementPool.addAll(poolManager.buildActivePool());
        this.promptManager = new AchievementPromptManager(activeAdvancementPool);
        this.scoreboard = new AchievementScoreboard(targetScore, playerScores);
    }

    public void start() {
        this.active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Select initial objective prompt
        promptManager.rotateObjective(getOnlinePlayers());

        // Start countdown timer if applicable
        if (gameMode == Settings.GameMode.HUNT || matchDurationSeconds > 0) {
            startTimer();
        }

        // Start Action Bar HUD & Scoreboard update ticker
        startHudTicker();
    }

    public void stop() {
        this.active = false;
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
            timerTask = null;
        }
        if (hudTask != null && !hudTask.isCancelled()) {
            hudTask.cancel();
            hudTask = null;
        }
        scoreboard.resetPlayers(getOnlinePlayers());
        HandlerList.unregisterAll(this);
    }

    private void startTimer() {
        this.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                if (remainingSeconds <= 0) {
                    cancel();
                    concludeSession("Match time elapsed");
                    return;
                }

                if (remainingSeconds == 60 || remainingSeconds == 30 || remainingSeconds == 10 || remainingSeconds <= 5) {
                    broadcastMessage(Component.text("Time remaining: ", NamedTextColor.YELLOW)
                            .append(Component.text(remainingSeconds + "s", NamedTextColor.GOLD, TextDecoration.BOLD)));
                }

                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startHudTicker() {
        this.hudTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                Collection<Player> players = getOnlinePlayers();
                if (promptManager.getCurrentObjective() == null) {
                    promptManager.rotateObjective(players);
                }
                String descriptionText = promptManager.getCurrentObjectiveDescription();

                // Update Scoreboard UI
                scoreboard.update(descriptionText, remainingSeconds, players);

                // Send Action Bar HUD
                Component actionBar = Component.text("Target: ", NamedTextColor.GOLD)
                        .append(Component.text(descriptionText, NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(" | Goal: ", NamedTextColor.GRAY))
                        .append(Component.text(targetScore + " pts", NamedTextColor.YELLOW));

                for (Player player : players) {
                    player.sendActionBar(actionBar);
                }
            }
        }.runTaskTimer(plugin, 10L, 20L);
    }

    public Collection<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(world)) {
                players.add(p);
            }
        }
        return players;
    }

    /**
     * Verifies and selects an uncompleted target advancement for a given player.
     * Uses player.getAdvancementProgress(advancement) to prevent soft-locking on already completed advancements.
     */
    public @Nullable Advancement selectNextEligibleAdvancement(Player player) {
        List<Advancement> candidatePool = new ArrayList<>();
        for (Advancement adv : activeAdvancementPool) {
            if (claimedAdvancements.contains(adv.getKey())) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            if (!progress.isDone()) {
                candidatePool.add(adv);
            }
        }

        if (candidatePool.isEmpty()) {
            return null;
        }

        return candidatePool.get(ThreadLocalRandom.current().nextInt(candidatePool.size()));
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!active) return;

        Player player = event.getPlayer();
        if (!player.getWorld().equals(world)) return;
        if (!playerUuids.contains(player.getUniqueId())) return;

        Advancement advancement = event.getAdvancement();
        NamespacedKey key = advancement.getKey();

        // Filter out recipes
        if (key.getKey().startsWith("recipes/")) return;

        // Check if advancement is in our active pool
        boolean isInPool = activeAdvancementPool.stream().anyMatch(a -> a.getKey().equals(key));
        if (!isInPool) return;

        // Avoid double-awarding for same advancement if already claimed
        if (!claimedAdvancements.add(key)) {
            return;
        }

        // Award points
        int newScore = playerScores.merge(player.getUniqueId(), 1, Integer::sum);
        plugin.getLogger().info("Player " + player.getName() + " completed advancement " + key + " (New Score: " + newScore + ")");

        // Update Scoreboard State
        scoreboard.updatePlayerScore(player.getUniqueId(), newScore);

        // Mark objective completed and rotate to next prompt
        promptManager.markCompleted(advancement);
        promptManager.rotateObjective(getOnlinePlayers());

        // Sound & Title Broadcast
        broadcastTitleAndSound(player, key);

        // Update UI immediately
        scoreboard.update(promptManager.getCurrentObjectiveDescription(), remainingSeconds, getOnlinePlayers());

        // Win Condition Check
        checkWinCondition(player, newScore);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!active) return;

        Player player = event.getEntity();
        if (!player.getWorld().equals(world)) return;
        if (!playerUuids.contains(player.getUniqueId())) return;

        plugin.getLogger().info("Player " + player.getName() + " died in session world '" + world.getName() + "'. Transitioning to spectator mode.");

        RGASessionControl sessionControl = plugin.getRgasessionControl();
        if (sessionControl != null) {
            sessionControl.setSpectator(player, true);
        } else {
            try {
                Object rgaInstance = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
                if (rgaInstance instanceof RGASessionControl sc) {
                    sc.setSpectator(player, true);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delegate spectator transition for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void checkWinCondition(Player player, int currentScore) {
        if (gameMode == Settings.GameMode.RACE) {
            if (currentScore >= targetScore) {
                // CPMK Solo QA Guard
                if (initialPlayerCount == 1) {
                    plugin.getLogger().info("[CPM] Single-player testing mode detected; suppressing automatic 0-opponent win condition for player " + player.getName());
                    broadcastMessage(Component.text("[QA Guard] Target score reached in single-player test mode.", NamedTextColor.LIGHT_PURPLE));
                    return;
                }

                concludeSession("Target score of " + targetScore + " reached by " + player.getName());
            }
        }
    }

    private void concludeSession(String reason) {
        if (!active) return;
        this.active = false;

        plugin.getLogger().info("Session in world '" + world.getName() + "' concluding. Reason: " + reason);

        RGASessionControl sessionControl = plugin.getRgasessionControl();
        if (sessionControl != null) {
            sessionControl.requestSessionConclude(world.getName(), reason, playerScores);
        } else {
            try {
                Object rgaInstance = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
                if (rgaInstance instanceof RGASessionControl sc) {
                    sc.requestSessionConclude(world.getName(), reason, playerScores);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to request session conclude from RGA: " + e.getMessage());
            }
        }
    }

    private void broadcastTitleAndSound(Player completedPlayer, NamespacedKey advancementKey) {
        Component titleComp = Component.text("Advancement Completed!", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component subComp = Component.text(completedPlayer.getName() + " completed ", NamedTextColor.YELLOW)
                .append(Component.text(advancementKey.getKey(), NamedTextColor.GREEN));

        Title title = Title.title(titleComp, subComp, Title.Times.times(
                Duration.ofMillis(300),
                Duration.ofMillis(2000),
                Duration.ofMillis(500)
        ));

        for (UUID uuid : playerUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(world)) {
                p.showTitle(title);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }

    private void broadcastMessage(Component message) {
        for (UUID uuid : playerUuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getWorld().equals(world)) {
                p.sendMessage(message);
            }
        }
    }

    public Map<UUID, Integer> getPlayerScores() {
        return Collections.unmodifiableMap(playerScores);
    }

    public World getWorld() {
        return world;
    }

    public boolean isActive() {
        return active;
    }

    public AchievementPromptManager getPromptManager() {
        return promptManager;
    }
}
