\# SimpleSkyblock



A custom Minecraft Spigot/Paper plugin for skyblock servers with advanced features.



\## Features



\- 🏝️ \*\*Island Management\*\* - Create, delete, and manage personal islands

\- 🛡️ \*\*Spawn Protection\*\* - Safe spawn lobby with PVP protection

\- 🤝 \*\*NPC Trading System\*\* - Config-based NPCs with custom trades

\- ⚔️ \*\*Custom Items\*\* - Weapons, tools, and armor with custom attributes

\- 🔒 \*\*Island Protection\*\* - 100x100 build limit with border protection

\- ⏰ \*\*Cooldown System\*\* - 7-day cooldown for island recreation



\## Requirements



\- Java 17+

\- Spigot/Paper 1.20.4+

\- Maven 3.6+



\## Installation



1\. Download the latest `.jar` from \[Releases](../../releases)

2\. Place in your server's `plugins/` folder

3\. Restart server

4\. Configure `items.yml` and `npcs\_config.yml` in `plugins/SimpleSkyblock/`



\## Commands



| Command | Description | Permission |

|---------|-------------|------------|

| `/is create` | Create new island | - |

| `/is home` | Teleport to your island | - |

| `/is delete` | Delete your island | - |

| `/is info` | Show island info | - |

| `/spawn` | Teleport to spawn | - |

| `/npc spawn <type>` | Spawn NPC | OP |

| `/npc remove` | Remove NPC | OP |

| `/admin \[gmc\\|gms\\|fly\\|god]` | Admin utilities | OP |

| `/restart` | Restart server | OP |



\## Configuration



\### items.yml

Define custom items with enchantments and attributes.



\### npcs\_config.yml

Configure NPCs, trades, and requirements.



See \[Wiki](../../wiki) for detailed configuration guide.



\## Building from Source





