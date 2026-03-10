# Chest Collector Plugin

A Hytale server plugin that adds automated item collection through special collector chests. Perfect for farms, mob grinders, and keeping your world tidy by automatically gathering nearby dropped items.

## Features

- **Collector Chests**: Place special chest blocks that automatically collect nearby dropped items
- **Configurable Radius**: Adjust collection radius per collector (up to server-defined maximum)
- **Per-Player Management**: Each player can manage their own collectors with individual settings
- **Item Filtering**: Configure which items each collector should pick up
- **Visual UI**: Easy-to-use interface for managing collectors and their settings
- **Notifications**: Get notified when items are collected (configurable display type)

## Commands

- `/collector get` - Get a collector chest item
- `/collector settings` - Open the collector management UI
- `/collector list` - List all your collectors
- `/collector help` - Show command help

## Configuration

The plugin's behavior can be customized in `config.yml`:

### Collector Settings

- **max-per-player**: Maximum number of collectors each player can place (default: 5)
- **default-radius**: Default collection radius for new collectors in blocks (default: 10)
- **max-radius**: Maximum allowed collection radius in blocks (default: 32)
- **collection-interval**: How often collectors scan for items in seconds (default: 2)
- **notification-type**: How players are notified when items are collected
  - `NOTIFICATION` - In-game popup notification (default)
  - `CHAT` - Chat message
  - `TITLE` - Title on screen
  - `NONE` - No notification

## Usage

1. Use `/collector get` to obtain a collector chest
2. Place the chest anywhere in the world
3. The chest will automatically start collecting nearby dropped items
4. Use `/collector settings` to:
   - View all your collectors
   - Adjust collection radius
   - Configure item filters
   - Enable/disable individual collectors
   - Delete collectors

## How It Works

- When you place a collector chest, it becomes active and begins scanning for dropped items
- Items within the configured radius are automatically picked up at regular intervals
- Collected items are stored in the chest's inventory
- You can configure filters to only collect specific items
- Each collector can be individually enabled/disabled without removing it
- Breaking a collector chest will remove it and drop its contents

## Requirements

- Hytale Server
- HyLib Commons library

## Installation

1. Place the plugin JAR file in your server's `plugins` folder
2. Start or restart your server
3. Configure settings in `config.yml` and `messages.yml` as desired
4. Reload or restart the server to apply changes

## Permissions
 
- `chestcollector.command.get` - Access to `/collector get` command
- `chestcollector.command.settings` - Access to `/collector settings` command
- `chestcollector.command.help` - Access to `/collector help` command
- `chestcollector.place` - Ability to place collector chests
