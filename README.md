# rga-achievementrace

A Companion Plugin Migration Kit (CPMK) compliant Paper minigame plugin for **Ronlab Game Assistant (RGA)**.

---

## Overview

`rga-achievementrace` brings the competitive Minecraft Advancement Race minigame experience to servers running Ronlab Game Assistant. Players race individually or in teams to complete randomly assigned Minecraft advancements within dedicated session worlds.

### Key Features

- **Dual Game Modes**:
  - `RACE`: First player/team to hit the target score wins immediately.
  - `HUNT`: Timed match where players tally as many completed objectives as possible before time expires.
- **Dynamic Objective Prompt Engine**:
  - Continuously assigns non-completed Minecraft advancements to players.
  - **Soft-Lock Prevention**: Verifies `player.getAdvancementProgress(advancement).isDone()` before target assignment to ensure pre-game advancements never soft-lock the session.
- **Action Bar HUD & Sidebar Scoreboard**:
  - Live 1-second Action Bar ticker (`Objective: <Advancement> | Goal: X pts`).
  - Dynamic sidebar scoreboard with number formatting clean-up (`NumberFormat.blank()`) showing live leaderboards, active target objective, and countdown timers.
- **CPMK Integration & Spectator Mode**:
  - Listens to RGA lifecycle events (`MinigameStartEvent` & `MinigameConcludeEvent`).
  - On player death in session worlds, delegates JIT spectator state management via `RGASessionControl.setSpectator(player, true)`.
  - **Solo QA Testing Guard**: Suppresses instant 0-opponent victory conditions when `initialPlayerCount == 1`.

---

## Prerequisites & Versioning

- **Java**: 25
- **Paper API**: `[26.2.build,)` (Minecraft 1.21.4+)
- **Ronlab Game Assistant**: `1.13.0`
- **Plugin Version**: `1.0.0-RGA`

---

## Configuration

The plugin uses `settings.yml` located in `plugins/rga-achievementrace/settings.yml`:

```yaml
# Achievement Race Configuration Settings
mode: RACE
target-score: 5
match-duration-seconds: 300
blacklist:
  - 'minecraft:husbandry/allay_deliver_cake_to_note_block'
  - 'minecraft:nether/all_potions'
```

### RGA Registration (`minigames.yml`)

Register `rga-achievementrace` in RGA's `minigames.yml`:

```yaml
ronlab:achievementrace:
  name: "Achievement Race"
  min-players: 1
  max-players: 16
  allow-spectators: true
  template-world: "turfwars"
```

---

## Building from Source

Build the plugin JAR using Apache Maven:

```bash
mvn clean package
```

The compiled output will be generated at `target/rga-achievementrace-1.0.0-RGA.jar`.

---

## Project Structure

```
AchievementRace/
├── .github/workflows/ci.yml   # GitHub Actions build pipeline (JDK 25)
├── .gitignore                 # Maven & IDE ignore rules
├── pom.xml                    # Maven POM dependency configuration
├── paper-plugin.yml           # Paper plugin manifest & RGA dependency declaration
├── settings.yml                # Game mode, target scores & blacklist configuration
└── src/main/java/com/ronlab/achievementrace/
    ├── AchievementRacePlugin.java    # Plugin entrypoint & RGA service binding
    ├── config/Settings.java          # Configuration parser & model
    ├── listener/RGAEventListener.java # MinigameStart / MinigameConclude listeners
    ├── session/
    │   ├── AchievementRaceSession.java   # Core game session logic & Bukkit listeners
    │   ├── AchievementPromptManager.java # Target objective selection & rotation
    │   └── SessionManager.java          # Multi-world session registry
    └── ui/AchievementScoreboard.java    # Sidebar scoreboard rendering
```

---

## License

See [LICENSE](LICENSE) for project license details.
