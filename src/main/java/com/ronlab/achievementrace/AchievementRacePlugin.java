package com.ronlab.achievementrace;

import com.ronlab.achievementrace.config.Settings;
import com.ronlab.achievementrace.listener.RGAEventListener;
import com.ronlab.achievementrace.session.SessionManager;
import com.ronlab.rga.api.RGASessionControl;
import com.ronlab.rga.api.model.MinigameId;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * Main Paper plugin class for rga-achievementrace.
 */
@NullMarked
public class AchievementRacePlugin extends JavaPlugin {

    public static final MinigameId GAME_ID = MinigameId.of("ronlab", "achievementrace");

    private static @Nullable AchievementRacePlugin instance;

    private @Nullable Settings settings;
    private @Nullable SessionManager sessionManager;
    private @Nullable RGASessionControl rgaSessionControl;

    @Override
    public void onEnable() {
        instance = this;

        // Enforce RGA dependency validation
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin == null || !rgaPlugin.isEnabled()) {
            getLogger().severe("RonlabGameAssistant (RGA) is required but not enabled! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (rgaPlugin instanceof RGASessionControl sessionControl) {
            this.rgaSessionControl = sessionControl;
        } else {
            getLogger().warning("RonlabGameAssistant does not implement RGASessionControl directly. Proceeding with fallback.");
        }

        // Load settings.yml configuration
        loadSettings();

        // Initialize Session Manager
        this.sessionManager = new SessionManager(this);

        // Register RGA event listener
        Bukkit.getPluginManager().registerEvents(new RGAEventListener(this), this);

        getLogger().info("rga-achievementrace successfully enabled for minigame: " + GAME_ID.asString());
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.terminateAllSessions();
        }
        getLogger().info("rga-achievementrace disabled.");
    }

    public void loadSettings() {
        File settingsFile = new File(getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            saveResource("settings.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(settingsFile);
        this.settings = new Settings(config);
        getLogger().info("Loaded settings.yml (Mode: " + settings.getMode() + ", Target Score: " + settings.getTargetScore() + ")");
    }

    public static AchievementRacePlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AchievementRacePlugin is not initialized.");
        }
        return instance;
    }

    public Settings getSettings() {
        if (settings == null) {
            throw new IllegalStateException("Settings are not loaded.");
        }
        return settings;
    }

    public SessionManager getSessionManager() {
        if (sessionManager == null) {
            throw new IllegalStateException("SessionManager is not initialized.");
        }
        return sessionManager;
    }

    public @Nullable RGASessionControl getRgasessionControl() {
        return rgaSessionControl;
    }
}
