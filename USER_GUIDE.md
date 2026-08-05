# rga-achievementrace — Companion User Guide & Architectural Specification

Official user guide and technical manual for **`rga-achievementrace`**, a Micro-Companion Architecture (CPMK) minigame plugin designed for **Ronlab Game Assistant (RGA)**.

---

## 1. Architectural Baseline & Specification Standard

`rga-achievementrace` integrates with `ronlabgameassistant` (`rga-core`) via the `rga-api` event bus.

### Target Environment & Dependencies
- **Java Release**: `Java 25` (`<maven.compiler.release>25</maven.compiler.release>`)
- **Server Platform**: `PaperMC 26.2` (`api-version: '26.2'`)
- **Primary Engine Baseline**: `rga-core` / `rga-api` version `1.13.0`
- **Plugin Identifier**: `ronlab:achievementrace`
- **Plugin Version**: `1.0.0-RGA`

---

## 2. Minigame Mechanics & Rulesets

In `rga-achievementrace`, players compete inside dedicated, dynamically generated RGA session worlds to complete Minecraft advancements.

### 2.1 Game Modes
- **`RACE` Mode**:
  - The objective engine randomly assigns target advancements to players.
  - The first player or team to hit the configured `target-score` (e.g., 5 points) wins the session immediately.
  - *Note*: If Solo QA Developer Mode is active, victory auto-termination is suppressed.
- **`HUNT` Mode**:
  - Players are given a countdown timer defined by `match-duration-seconds` (e.g., 300 seconds / 5 minutes).
  - Players race to complete as many assigned target advancements as possible before time expires.
  - The player or team with the highest point total when time runs out wins.

### 2.2 Objective Rotation & Soft-Lock Prevention
- The `AchievementPromptManager` picks random advancements from the active server pool (excluding recipes and blacklisted entries).
- **Soft-Lock Prevention**: Before offering an advancement target to a player, the prompt engine evaluates `player.getAdvancementProgress(advancement).isDone()`. If the player already unlocked that advancement prior to or during the session, it is skipped to prevent soft-locking.

### 2.3 User Interface (HUD & Scoreboard)
- **Action Bar HUD**: Updated every second via a Bukkit ticker task:
  ```
  Target: <Advancement Description> | Goal: X pts
  ```
- **Sidebar Scoreboard**:
  - Displays session title (`ACHIEVEMENT RACE`), current active target objective, target score goal, sorted player leaderboard, and match countdown timer.
  - **PaperMC Number Formatting**: Applies `objective.numberFormat(NumberFormat.blank())` across all sidebar lines to eliminate default red score margin numbers.
  - **Post-Teleport Assignment**: Scoreboard assignment (`player.setScoreboard(...)`) occurs strictly during post-teleport spawn phases to prevent chunk-loading hangs.
  - **Teardown Routine**: Upon session conclusion (`MinigameConcludeEvent`), scoreboards restore main scoreboard (`player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard())`) and unregister objectives (`objective.unregister()`).

### 2.4 Spectator & Death Handling
- On player death in a session world (`PlayerDeathEvent`), `rga-achievementrace` intercepts the event and delegates JIT spectator management to RGA via `RGASessionControl.setSpectator(player, true)`.

---

## 3. CPMK Event Bus Integration

`rga-achievementrace` is built to be companion-type agnostic. It does not access `rga-core` internal classes directly, communicating exclusively over the `rga-api` event bus.

### Event Handlers (`RGAEventListener`)
1. **`MinigameStartEvent`**:
   - Triggered when `rga-core` launches an Achievement Race session.
   - Evaluates `event.getMinigameId()`. If matching `ronlab:achievementrace`, calls `SessionManager.createSession(event.getWorldName(), event.getPlayerUuids())`.
2. **`MinigameConcludeEvent`**:
   - Triggered when `rga-core` signals session conclusion.
   - Aggregates final player scores into `event.getScores()`.
   - Cleans up tasks, scoreboards, and active session objects via `SessionManager.removeSession(event.getWorldName())`.

---

## 4. Solo QA Developer Mode (`initialPlayerCount == 1`)

To facilitate rapid iteration, map validation, and mechanics testing, `rga-achievementrace` supports **Solo QA Developer Mode**.

### Solo QA Behavior
- **Trigger**: Automatically engaged whenever a session starts with exactly 1 player (`initialPlayerCount == 1`).
- **Freeze Win Condition**: When the single player reaches or exceeds `target-score`, automatic match conclusion is frozen/suppressed.
- **Console Log**:
  ```
  [CPM] Single-player testing mode detected; suppressing automatic 0-opponent win condition for player <PlayerName>
  ```
- **In-Game Notification**: Broadcasts a light purple chat notification to the tester:
  ```
  [QA Guard] Target score reached in single-player test mode.
  ```
- **Purpose**: Enables continuous testing of map resets, void fall thresholds, objective rotation algorithms, and scoreboard rendering without the server evicting the developer from the test world.

---

## 5. Configuration, Administrative Commands & Permissions

### 5.1 Configuration Reference (`config.yml` / `settings.yml`)

| Key | Type | Default | Description |
|---|---|---|---|
| `mode` | String | `RACE` | Minigame mode (`RACE` or `HUNT`). |
| `target-score` | Integer | `5` | Points required for victory in `RACE` mode. |
| `match-duration-seconds` | Integer | `300` | Countdown duration in seconds (0 = unlimited). |
| `blacklist` | List\<String\> | See config | List of advancement keys excluded from rotation. |

#### Vector & Threshold Provisioning (Template World)
Spawn vectors, pedestal positions, and fall Y-thresholds are defined in RGA's template world configuration (`turfwars` template):
- **Spawn Vector**: `(X: 0.5, Y: 64.0, Z: 0.5, Pitch: 0.0, Yaw: 0.0)`
- **Pedestal Platform**: World spawn platform center.
- **Fall Threshold**: `Y = -64` (triggers spawn reset or spectator transition).

### 5.2 RGA Integration Configuration (`minigames.yml`)
Add the following companion registration entry into RGA's `minigames.yml`:

```yaml
ronlab:achievementrace:
  name: "Achievement Race"
  min-players: 1
  max-players: 16
  allow-spectators: true
  template-world: "turfwars"
```

### 5.3 Commands & Permission Nodes
Administrative lifecycle management (starting, stopping, queueing sessions) is managed by `rga-core` administrative commands:
- `/rga minigame start ronlab:achievementrace <world>`: Starts an Achievement Race match.
- `/rga minigame stop <world>`: Manually concludes an active session.

#### Local Companion Permission Nodes
- `rga.companion.achievementrace.admin`: Grants permission to administrative debug options and manual config reloads.
- `rga.companion.achievementrace.play`: Grants permission to join Achievement Race sessions.
