# ChestCollector

ChestCollector is a Hytale server plugin that allows players to place special chests that automatically collect nearby dropped items. This is perfect for automated farms, mob grinders, or simply keeping areas tidy.

## Features

- **Automated Collection**: Special chests automatically pick up items within a configurable radius.
- **Configurable Radius**: Players can adjust the collection radius (up to a server-defined maximum).
- **Item Filtering**: (Planned) Filter which items are collected or ignored.
- **Visual Feedback**: Optional notifications when items are collected.
- **Easy Management**: Simple commands and UI to manage collectors.

## Configuration

The `config.yml` file allows you to customize the plugin's behavior:

```yaml
collector:
  # Maximum number of collectors each player can place
  max-per-player: 5
  
  # Default collection radius (in blocks) for new collectors
  default-radius: 10
  
  # Maximum collection radius (in blocks) allowed
  max-radius: 32
  
  # How often (in seconds) collectors scan for items
  collection-interval: 2
  
  # Notification type: NOTIFICATION, CHAT, TITLE, or NONE
  notification-type: "NOTIFICATION"
```

## Commands

| Command                    | Description                                     | Permission           |
|----------------------------|-------------------------------------------------|----------------------|
| `/collector get`           | Gives the player a collector chest item.        | `chestcollector.use` |
| `/collector list`          | Lists all collectors owned by the player.       | `chestcollector.use` |
| `/collector settings <id>` | Opens the settings UI for a specific collector. | `chestcollector.use` |
| `/collector help`          | Shows the help menu.                            | `chestcollector.use` |

## Usage

1. **Get a Collector**: Use `/collector get` to receive a special collector chest.
2. **Place it**: Place the chest anywhere in the world.
3. **Configure**: Right-click the chest (or use `/collector settings`) to open the configuration menu.
   - Adjust the radius.
   - Enable/Disable collection.
4. **Enjoy**: Dropped items within the radius will be automatically moved into the chest.

## Permissions

- `chestcollector.use`: Allows players to use collector commands and place collector chests.
- `chestcollector.admin`: (Planned) Allows admins to manage other players' collectors.

## Support

If you encounter any issues or have feature requests, please join the [BusyBee Discord](discord.gg/abdm29q7af)
