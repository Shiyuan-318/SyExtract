# SyExtract

A red envelope plugin for Minecraft Paper/Purpur 1.21 with Vault economy support.

## Features

- Create red envelopes (set name, amount, quantity)
- **Each red envelope has a unique numeric ID starting from 1 (e.g., 1, 2, 3)**
- GUI interface for viewing and claiming red envelopes
- **GUI can be opened even when no red envelopes are available**
- **Broadcast red envelopes to all players with one click**
- **Unclaimed amounts are automatically refunded when red envelopes expire**
- **Multi-language support (简体中文/English)**
- Vault economy system support
- Configurable fee system
- Player ban system (restrict sending/claiming red envelopes)
- Red envelope expiration settings
- Highly configurable

## Requirements

- Minecraft Server: Paper/Purpur 1.21
- Java: 17 or higher
- Dependencies: Vault + any economy plugin (e.g., EssentialsX)

## Commands

| Command                               | Permission          | Description                             |
| ------------------------------------- | ------------------- | --------------------------------------- |
| `/sye create <name> <amount> <count>` | syextract.create    | Create a red envelope                   |
| `/sye open`                           | syextract.open      | Open the red envelope GUI               |
| `/sye broadcast <id>`                 | syextract.broadcast | Broadcast a red envelope to all players |
| `/sye claim <id>`                     | syextract.claim     | Claim a specific red envelope by ID     |
| `/sye ban <player> <time>`            | syextract.ban       | Ban a player                            |
| `/sye unban <player>`                 | syextract.unban     | Unban a player                          |
| `/sye reload`                         | syextract.reload    | Reload configuration                    |

### Command Aliases

- `create` → `c`
- `open` → `o`
- `broadcast` → `br`
- `claim` → `cl`
- `ban` → `b`
- `unban` → `u`
- `reload` → `r`

### Time Format

Ban duration supports the following formats:

- `1h` - 1 hour
- `1d` - 1 day
- `1w` - 1 week
- `30d` - 30 days

## Permissions

| Permission          | Default | Description                       |
| ------------------- | ------- | --------------------------------- |
| syextract.use       | true    | Basic usage permission            |
| syextract.create    | true    | Create red envelope permission    |
| syextract.open      | true    | Open GUI permission               |
| syextract.claim     | true    | Claim red envelope permission     |
| syextract.broadcast | true    | Broadcast red envelope permission |
| syextract.ban       | op      | Ban player permission             |
| syextract.unban     | op      | Unban player permission           |
| syextract.reload    | op      | Reload configuration permission   |
| syextract.admin     | op      | All admin permissions             |

## Configuration

### config.yml

```yaml
# Language Settings
# Available values: zh (简体中文), en (English)
language: zh

# Red envelope settings
red-envelope:
  # Default claim duration (hours)
  default-expire-hours: 24
  # Minimum red envelope amount
  min-amount: 1.0
  # Minimum red envelope count
  min-count: 1
  # Maximum red envelope count
  max-count: 100

# Fee settings
fee:
  # Enable fee
  enabled: true
  # Fee percentage (0.0 - 1.0, e.g., 0.05 = 5%)
  percentage: 0.05
  # Minimum fee
  min-fee: 1.0

# GUI settings
gui:
  # Red envelope item material
  item-material: "SUNFLOWER"

# Broadcast message settings
broadcast:
  # Notification when red envelope is fully claimed
  envelope-completed: "&6&l🎉 Red envelope &e{name} &7has been fully claimed! &6{player} &7got the highest amount: &e{amount} &7coins!"
```

### Language Files

The plugin supports multi-language through language files located in `plugins/SyExtract/lang/`:

- `zh.yml` - 简体中文 (default)
- `en.yml` - English

You can customize all messages in these files. To switch language, change the `language` option in `config.yml`.

#### Language File Structure

```yaml
# Message prefix
prefix: "&8[&cSyExtract&8] &r"

# Success messages
success:
  create: "&aSuccessfully created red envelope! ID: &e{id}"
  claim: "&aYou claimed the red envelope and received &6{amount} &acoins!"
  # ... more messages

# Error messages
error:
  no-permission: "&cYou don't have permission!"
  # ... more messages

# GUI texts
gui:
  title: "&c&lRed Envelope Hall"
  # ... more texts

# Broadcast messages
broadcast:
  format: "&6&l🧧 Red Envelope! &e{sender} &7sent &6&l[{name}]"
  click-text: "&a&l[Claim Now]"
```

## Data Storage

The plugin generates the following data files in the `plugins/SyExtract/` directory:

- `envelopes.yml` - Red envelope data
- `bans.yml` - Ban data
- `lang/` - Language files (auto-generated)

## Usage Examples

1. **Create a red envelope**
   ```
   /sye create HappyNewYear 1000 10
   ```
   Create a red envelope named "HappyNewYear" with a total of 1000 coins divided into 10 shares.
   The system will display the red envelope ID upon successful creation.

2. **Open GUI to claim red envelopes**
   ```
   /sye open
   ```
   The GUI can be opened even when no red envelopes are available.

3. **Broadcast a red envelope**
   ```
   /sye broadcast 1
   ```
   Broadcast the red envelope with ID "1" to all online players.
   Players can click the "[Claim Now]" button in the chat to claim it.

4. **Claim a specific red envelope**
   ```
   /sye claim 1
   ```
   Directly claim the red envelope with ID "1".

5. **Ban a player**
   ```
   /sye ban PlayerName 7d
   ```
   Ban a player for 7 days, preventing them from sending and claiming red envelopes.

6. **Unban a player**
   ```
   /sye unban PlayerName
   ```

## Notes

1. Creating a red envelope deducts the envelope amount + fee
2. Red envelopes use a random distribution algorithm; each share amount is random
3. **Expired unclaimed red envelopes will be automatically refunded to the sender**
4. Each player can only claim the same red envelope once
5. **Each red envelope has a unique numeric ID for easy sharing and claiming**
6. **Multi-language support allows you to customize all plugin messages**

## Changelog

### v1.4.0

- Added multi-language support (简体中文/English)
- Language files are now stored in `lang/` folder
- All messages can be customized through language files
- Added `language` option in config.yml

### v1.3.0

- Added notification when red envelope is fully claimed
- Shows the luckiest player (hand king) who got the highest amount
- Added `broadcast.envelope-completed` message configuration

### v1.2.0

- Fixed broadcast message not found issue (success.broadcast)
- Fixed clickable claim button in broadcast messages
- Changed red envelope ID from random 6-character to sequential numeric ID starting from 1

### v1.1.0

- Added unique ID for each red envelope
- Added automatic refund for expired unclaimed red envelopes
- Added `/sye broadcast` command to broadcast red envelopes
- Added `/sye claim` command to claim by ID
- GUI can now be opened even when no red envelopes are available
- Updated to Java 17 compatibility

### v1.0.0

- Initial release

## Author

- **Author**: Shiyuan
- **Year**: 2026
