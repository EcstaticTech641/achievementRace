# rga-achievementrace

A Micro-Companion Architecture (CPMK) compliant Paper minigame plugin for **Ronlab Game Assistant (RGA)**.

---

## Overview

`rga-achievementrace` brings the competitive Minecraft Advancement Race minigame experience to servers running Ronlab Game Assistant (`rga-core`). Players race individually or in teams to complete randomly assigned Minecraft advancements within dedicated session worlds.

Detailed architectural documentation, testing guides, and command references are available in the [User Guide](USER_GUIDE.md).

---

## CPMK Architectural Alignment (5 Pillars)

1. **Core Gameplay Function Retention**:
   - Preserves 100% of native Advancement Race mechanics, custom objective selection loops, and Action Bar HUD tickers.
2. **Ronlab Integration Standard**:
   - Listens strictly to CPMK event payloads (`MinigameStartEvent` and `MinigameConcludeEvent`).
   - Manifest `paper-plugin.yml` specifies `api-version: '26.2'`, declares `RonlabGameAssistant` under `dependencies.server` with `required: true` and `join-classpath: true`, and contains no invalid `load: BEFORE` directives.
3. **Baseline Structure & Rules Provision**:
   - Scoreboard lines use PaperMC's `objective.numberFormat(NumberFormat.blank())` to suppress margin red numbers.
   - Scoreboards are assigned post-teleport to prevent chunk-loading hangs.
   - Teardown routines restore the main scoreboard (`player.setScoreboard(main)`) and unregister objectives on `MinigameConcludeEvent`.
4. **Companion-Type Agnostic Design**:
   - Operates as a self-contained companion module decoupled from `rga-core` internals, communicating solely over the `rga-api` event bus.
5. **Feature Implementation & Modification Specs**:
   - Supports **Solo QA Developer Mode** (`initialPlayerCount == 1`) which freezes win conditions when a single player hits target scores, permitting uninterrupted map reset and mechanics testing.

---

## Prerequisites & Versioning

- **Java**: 25 (`<maven.compiler.release>25</maven.compiler.release>`)
- **Paper API**: `26.2` (`[26.2.build,)`)
- **Ronlab Game Assistant (RGA)**: `1.13.0` (`rga-api` 1.13.0)
- **Plugin Version**: `1.0.0-RGA`

---

## Configuration

The plugin loads configuration settings from `config.yml` / `settings.yml`:

```yaml
# Gamemode Selection: RACE or HUNT
mode: RACE

# Target Score required to win in RACE mode
target-score: 5

# Match Duration in seconds (300 = 5 minutes)
match-duration-seconds: 300

# Blacklisted Minecraft advancements
blacklist:
  - "minecraft:adventure/avoid_vibration"
  - "minecraft:nether/all_potions"
  - "minecraft:nether/create_beacon"
  - "minecraft:nether/all_effects"
  - "minecraft:husbandry/complete_catalogue"
  - "minecraft:husbandry/allay_deliver_cake_to_note_block"
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

## Solo QA Developer Mode

When starting a session with 1 player (`initialPlayerCount == 1`), **Solo QA Developer Mode** is activated automatically:
- Target score win conditions freeze upon achievement to prevent match termination.
- Logs `[CPM] Single-player testing mode detected...` and broadcasts `[QA Guard] Target score reached in single-player test mode.` to chat.
- Allows continuous map reset, fall threshold, and mechanics testing without session eviction.

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
├── USER_GUIDE.md              # Detailed companion manual & CPMK specification
├── README.md                  # Companion overview & build instructions
└── src/main/
    ├── resources/
    │   ├── paper-plugin.yml   # Paper plugin manifest & RGA dependency declaration
    │   ├── config.yml         # Default game configuration & Solo QA comments
    │   └── settings.yml       # Configuration fallback
    └── java/com/ronlab/achievementrace/
        ├── AchievementRacePlugin.java    # Plugin entrypoint & RGA service binding
        ├── config/Settings.java          # Configuration parser & model
        ├── listener/RGAEventListener.java # MinigameStart / MinigameConclude listeners
        ├── session/
        │   ├── AchievementRaceSession.java   # Core game session logic & Bukkit listeners
        │   ├── AchievementPromptManager.java # Target objective selection & rotation
        │   ├── AdvancementPoolManager.java   # Advancement filtering & blacklist
        │   └── SessionManager.java          # Multi-world session registry
        └── ui/AchievementScoreboard.java    # Sidebar scoreboard rendering & teardown
```

---

## License

See [LICENSE](LICENSE) for project license details.
