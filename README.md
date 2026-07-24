# 🌾 FarmContest (v2.0.0)

<div align="center">

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4--1.21.x-brightgreen?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Purpur-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Dependencies](https://img.shields.io/badge/Dependencies-Vault%20%7C%20PlaceholderAPI-yellow?style=for-the-badge)

**The ultimate Hypixel Jacob's Contest-style farming event & crop mutation plugin for your Minecraft server!**

[Features](#-key-features) • [Installation](#-installation-guide) • [Commands & Permissions](#-commands--permissions) • [Placeholders](#-placeholderapi-integration) • [Compatibility](#-compatibility)

---

</div>

## 🌟 Key Features

### 🏆 1. Automated Farming Contest System (Farm Contest)
* **Automatic Rotation:** Automatically starts/ends contests and randomly selects crops (Sugar Cane, Cactus, Pumpkin, Melon, Wheat...) on a scheduled timer.
* **Real-time Leaderboard:** Calculates scores, updates rankings, and distributes rewards with absolute accuracy for every player.
* **Intuitive BossBar Display:** Displays a live countdown timer and current score progress directly on screen.
* **Smart Anti-Cheat:** Tracks and remembers player-placed blocks to prevent infinite point exploits from placing and breaking the same crop.

---

### 🧬 2. Mutation System & Shop GUI
* **Random Mutations:** Harvesting crops gives a chance to yield mutated crops with ultra-rare attributes and massive score multipliers.
* **Biome Dependence:** Certain mutation types only appear in specific biomes, encouraging players to explore the world.
* **Mutation Shop:** Sleek GUI interface allowing players to buy, sell, trade, and upgrade mutated seeds/crops.

---

### 💾 3. Data Storage & Flexible Integrations
* **Cross-Platform Database:** Supports **SQLite** (default, zero extra setup required).
* **Seamless Sync:** Automatically loads/saves player data on Join and Quit without causing server lag.
* **Vault Integration:** Reward players with in-game money directly into their balance upon winning.
* **PlaceholderAPI Integration:** Effortlessly display scores and ranks on Scoreboards, Tablists, Holograms, or Chat formats.

---

## 🛠 Installation Guide

1. Download the latest `FarmContest-2.0.0.jar` file from the **Releases** section.
2. Drop the `.jar` file into your server's `/plugins/` directory.
3. *(Recommended)* Install optional dependency plugins:
   * **[Vault](https://www.spigotmc.org/resources/vault.3431/)** (For economy rewards).
   * **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** (For displaying stats on Scoreboards/Tab).
4. Restart your server to generate the default configuration files.

---

## 📜 Commands & Permissions

Main command: `/farmcontest` or alias `/fc`

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/fc help` | View the list of available commands | `farmcontest.use` |
| `/fc status` | View active contest status & personal score | `farmcontest.use` |
| `/fc shop` | Open the Mutation Crops Shop | `farmcontest.use` |
| `/fc top` | View the current contest leaderboard | `farmcontest.use` |
| `/fc start <crop>` | *(Admin)* Forcefully start a farming contest | `farmcontest.admin` |
| `/fc stop` | *(Admin)* Instantly stop the current contest | `farmcontest.admin` |
| `/fc reload` | *(Admin)* Reload all plugin configuration files | `farmcontest.admin` |

---

## 📊 PlaceholderAPI Integration

Use the following placeholders in your Scoreboards (FeatherBoard, TAB...), Chat, or Holograms:

* `%farmcontest_status%` — Contest status (*Active / Finished*).
* `%farmcontest_time_remaining%` — Remaining time of the active contest.
* `%farmcontest_crop%` — Crop type featured in the current round.
* `%farmcontest_score%` — Player's current contest score.
* `%farmcontest_position%` — Player's current position on the leaderboard.

---

## 🖥 Compatibility

* **Supported Minecraft Versions:** `1.20.4` $
ightarrow$ `1.21.x` (and future updates).
* **Software:** Paper, Spigot, Purpur, Leaf, Folia...
* **Note:** Built entirely on standard API with no NMS/Reflection dependencies, making it extremely stable and safe across future Minecraft updates!

---

<div align="center">

Made with ❤️ for Minecraft Server Owners.

</div>
