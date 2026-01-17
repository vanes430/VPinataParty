# VPinataParty 🪅

[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-00BFFF?style=for-the-badge)](https://papermc.io)
[![Version](https://img.shields.io/badge/Version-1.0--SNAPSHOT-green?style=for-the-badge)](#)
[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)](#)

**VPinataParty** is a high-performance, enterprise-grade Pinata plugin specifically engineered for modern Minecraft server environments. It features native **Folia** regional threading support and a robust in-memory hit-tracking system, ensuring maximum stability and performance even under heavy player loads.

---

## 🚀 Key Features

*   **Native Folia Support**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`. Spawning and logic execution are thread-safe across regional boundaries.
*   **Virtual Health System**: Hits are tracked in-memory using a thread-safe `ConcurrentHashMap`. The entity's vanilla health is hardcoded, preventing accidental despawns from non-player damage.
*   **Dynamic Scaling**: Automatically scales Pinata health based on real-time online player counts to maintain challenge and economic balance.
*   **Advanced Formatting**: Full integration with **Kyori MiniMessage**. Supports hex colors, gradients, and modern text decorators.
*   **Anti-Spam Cooldown**: Configurable per-entity invulnerability ticks to prevent macro/autoclicker exploitation.
*   **Professional Entity Namespacing**: Uses standard `minecraft:entity_id` format for professional configuration management.

---

## 🛠 Installation

1.  Download the latest `VPinataParty.jar`.
2.  Place the file into your server's `plugins` directory.
3.  **Dependency**: Ensure [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) is installed for dynamic reward placeholders.
4.  Restart the server to generate default configurations.

---

## 💻 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/pinata summon` | Summon a Pinata at your current location. | `vpinataparty.admin` |
| `/pinata summon <id>` | Summon a Pinata at a pre-defined location. | `vpinataparty.admin` |
| `/pinata party` | Trigger a server-wide party at all locations. | `vpinataparty.admin` |
| `/pinata reload` | Hot-reload all configurations and messages. | `vpinataparty.admin` |

---

## ⚙️ Configuration

### `config.yml`
The main configuration file handles entity settings and health scaling logic.

```yaml
pinata:
  display-name: "<gradient:red:yellow><b>PINATA</b></gradient>"
  type: "minecraft:llama"
  no-ai: true
  # In-memory health scaling
  health-scaling:
    min-health: 20      # Hits required for 1-5 players
    max-health: 200     # Hits cap for 50+ players
    min-players: 5
    max-players: 50
  tick-hit-cooldown: 10 # Prevents damage spam (20 ticks = 1s)
  rewards:
    - chance: 100.0
      commands: ["msg %player_name% <yellow>You hit the pinata!"]
```

### `messages.yml`
Localization and system messages using MiniMessage format.

```yaml
prefix: "<gradient:gold:yellow><b>VPinataParty</b></gradient> <dark_gray>» <gray>"
reload-success: "<green>Configuration and messages reloaded!"
```

---

## 🏗 Technical Architecture

*   **Concurrency**: Uses `ConcurrentHashMap` for hit tracking to ensure thread safety in multi-threaded environments like Folia.
*   **Scheduling**: Spawning is handled via `RegionScheduler`; reward execution is offloaded to the `GlobalRegionScheduler`.
*   **Data Integrity**: Entity states are marked with `PersistentDataContainer` (PDC), ensuring logic persists through regional reloads.
*   **Event Priority**: Damage listeners use `ignoreCancelled = true` to respect third-party protection plugins (WorldGuard, etc.).

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.