# 🐝 ChestCollection
**Automated item collection through special collector chests for Hytale servers.**

ChestCollection is a powerful and lightweight Hytale plugin that introduces automated item collection. Players can place special "Collector Chests" that scan the surrounding area and automatically vacuum up dropped items. It's the perfect solution for automated farms, mob grinders, and keeping your server clean from item lag.

---

## ✨ Key Features
- **🔸 Automated Collection**: Automatically picks up nearby dropped items and stores them in the chest.
- **🔸 Custom Filters**: Advanced UI to filter which items should be collected.
- **🔸 Adjustable Radius**: Control exactly how far each collector scans (up to 32 blocks).
- **🔸 Dynamic Notifications**: Choose how you want to be notified (Popup, Title, Chat, or Silent).
- **🔸 Limits & Performance**: Configurable limits per player and collection intervals to ensure server stability.
- **🔸 Professional UI**: Sleek, custom-designed Hytale UI for managing all your collectors.

---

## 🚀 Getting Started

### Installation
1. Download the latest `ChestCollector.jar`.
2. Place the jar file into your Hytale server's `plugins` folder.
3. Restart your server to generate the configuration files.
4. Configure the settings in `config.yml` to your liking.

### How to use
1. Use `/collector get` to receive a Collector Chest.
2. Place the chest down in the world.
3. Interact with the chest or use `/collector settings` to configure its radius and filters.
4. Watch as it starts buzzing and collecting items!

---

## 📜 Commands

| Command               | Permission                        | Description                                  |
|:----------------------|:----------------------------------|:---------------------------------------------|
| `/collector help`     | `chestcollector.command.help`     | Displays the help menu.                      |
| `/collector get`      | `chestcollector.command.get`      | Gives you a special Collector Chest.         |
| `/collector settings` | `chestcollector.command.settings` | Opens the management UI for your collectors. |
| `/collector list`     | `chestcollector.command.settings` | Shows a list of all your placed collectors.  |

---

## 🔑 Permissions

| Permission Node                   | Default    | Description                                         |
|:----------------------------------|:-----------|:----------------------------------------------------|
| `chestcollector.command.help`     | `Everyone` | Access to the help command.                         |
| `chestcollector.command.get`      | `Everyone` | Ability to obtain a collector chest.                |
| `chestcollector.command.settings` | `Everyone` | Access to the settings and list UI.                 |
| `chestcollector.place`            | `Everyone` | Permission to place a collector chest in the world. |

---

## ⚙️ Configuration

The plugin is highly customizable through the `config.yml` file.

```yaml
collector:
  # Maximum number of collectors each player can place
  max-per-player: 5

  # Default collection radius (in blocks) for new collectors
  default-radius: 10

  # Maximum collection radius allowed (capped for performance)
  max-radius: 32

  # How often (in seconds) collectors scan for items
  collection-interval: 2

  # Notification type (NOTIFICATION, CHAT, TITLE, NONE)
  notification-type: "NOTIFICATION"
```

---

## 🛠️ Developers & Support
- **Author**: djtmk (BusyBee)
- **Discord**: [Join our community](https://discord.gg/abdm29q7af)
- **Website**: [CurseForge Page](https://www.curseforge.com/hytale/mods/chestcollector)
