package com.ronlab.achievementrace.session;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.advancement.Advancement;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the active target advancement objective for an AchievementRaceSession.
 */
@NullMarked
public class AchievementPromptManager {

    private final List<Advancement> fullPool;
    private final Set<String> completedKeys = new HashSet<>();
    private @Nullable Advancement currentObjective;

    public AchievementPromptManager(List<Advancement> fullPool) {
        this.fullPool = List.copyOf(fullPool);
    }

    /**
     * Selects and sets the next eligible target advancement for a given player or session.
     * Verifies that the player has not already completed the advancement.
     */
    public @Nullable Advancement rotateObjective(Collection<Player> players) {
        List<Advancement> eligible = new ArrayList<>();

        for (Advancement adv : fullPool) {
            String keyStr = adv.getKey().toString().toLowerCase();
            if (completedKeys.contains(keyStr)) {
                continue;
            }

            // If players collection is empty (e.g., during start before teleport finishes),
            // treat all non-completed pool advancements as eligible.
            boolean availableForAnyPlayer = players.isEmpty();
            if (!availableForAnyPlayer) {
                for (Player player : players) {
                    AdvancementProgress progress = player.getAdvancementProgress(adv);
                    if (!progress.isDone()) {
                        availableForAnyPlayer = true;
                        break;
                    }
                }
            }

            if (availableForAnyPlayer) {
                eligible.add(adv);
            }
        }

        if (eligible.isEmpty()) {
            this.currentObjective = null;
            return null;
        }

        Advancement next = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        this.currentObjective = next;
        return next;
    }

    public void markCompleted(Advancement adv) {
        completedKeys.add(adv.getKey().toString().toLowerCase());
    }

    public @Nullable Advancement getCurrentObjective() {
        return currentObjective;
    }

    /**
     * Parses and returns the descriptive instruction criteria ("how to get") of the current objective.
     * Uses PlainTextComponentSerializer to safely convert Kyori Adventure Components into plain text.
     * Falls back to key name if description is null or empty.
     */
    public String getCurrentObjectiveDescription() {
        if (currentObjective == null) {
            return "None Available";
        }

        AdvancementDisplay display = currentObjective.getDisplay();
        if (display != null && display.description() != null) {
            String descriptionText = PlainTextComponentSerializer.plainText().serialize(display.description()).trim();
            if (!descriptionText.isBlank()) {
                return descriptionText;
            }
        }

        return getFallbackKeyName(currentObjective);
    }

    /**
     * Returns the advancement display title or fallback key name.
     */
    public String getCurrentObjectiveDisplayName() {
        if (currentObjective == null) {
            return "None Available";
        }

        AdvancementDisplay display = currentObjective.getDisplay();
        if (display != null && display.title() != null) {
            String titleText = PlainTextComponentSerializer.plainText().serialize(display.title()).trim();
            if (!titleText.isBlank()) {
                return titleText;
            }
        }

        return getFallbackKeyName(currentObjective);
    }

    private String getFallbackKeyName(Advancement adv) {
        String key = adv.getKey().getKey();
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash != -1) {
            key = key.substring(lastSlash + 1);
        }
        String[] words = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public Component getCurrentObjectiveComponent() {
        return Component.text(getCurrentObjectiveDescription(), NamedTextColor.GREEN, TextDecoration.BOLD);
    }
}
