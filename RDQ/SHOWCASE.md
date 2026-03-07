# RDQ Showcase

RDQ (RaindropQuests) is a modern Minecraft progression plugin for Paper, Spigot, and Folia servers that combines **Quests**, **Perks**, **Ranks**, and **Bounties** into one connected gameplay system.

If you are searching for a **Minecraft quests plugin**, **Minecraft rank progression plugin**, **Minecraft bounty hunting plugin**, or **Minecraft perks plugin**, RDQ is built for that exact server experience.

Guide Video Placeholder: `[INSERT_GUIDE_VIDEO_URL_HERE]`  
Showcase Video Placeholder: `[INSERT_SHOWCASE_VIDEO_URL_HERE]`

## Why RDQ

RDQ is designed for servers that want long-term player retention, clear progression loops, and real gameplay identity.

- Four progression systems under one plugin: quests, perks, ranks, and bounties.
- GUI-first design with player-facing views and admin management views.
- Database-backed architecture for persistent progression and scalable growth.
- Optional economy, permissions, placeholder, skills, and jobs integrations.
- Built as part of the **RaindropCentral.com** ecosystem.

## International Language Support

- RDQ ships with `24` included languages (including English).
- Language output is player-specific and follows each player's Minecraft client language setting.
- Server owners do not need to configure one global language for everyone.
- To add or customize translations, place language `.yml` files in `plugins/RDQ/translations/`.
- Multilingual support works automatically for mixed-language player bases.

## The 4 Major Systems

### 1) Quests (In Development)

RDQ includes an in-plugin quest route and main-menu slot for the quest experience, and the quest system is currently in active development.

- `/rq quests` route is reserved and permission-gated with raindropquests.command.quests
- Quests are being developed as a core progression pillar alongside ranks, perks, and bounties.
- Designed for servers that want objective-based progression, repeatable engagement, and structured player goals.

This is ideal for communities that want to launch with ranks/perks/bounties now and roll quests in as the system matures.

### 2) Perks

Perks let players build playstyle identity with unlockable and toggleable advantages.

- `/rq perks` route is reserved and permission-gated with raindropquests.command.perks
- 21 built-in perk definitions are included by default.
- Supports both **PASSIVE** and **EVENT_TRIGGERED** perk types.
- Categories include combat, survival, movement, utility, and cosmetic styles.
- Unlock logic supports requirement-based progression and reward hooks.
- Perks can be granted from rank rewards using perk reward integration.
- Sidebar support allows a dedicated perks scoreboard toggle with `/rq scoreboard perks`.

Perks are ideal for increasing session time, encouraging specialization, and giving players reasons to keep progressing.

### 3) Ranks

Ranks provide the long-form progression backbone of RDQ with tree-based advancement.

- `/rq ranks` route is reserved and permission-gated with raindropsquests.command.ranks
- Includes six default rank trees: `warrior`, `ranger`, `mage`, `rogue`, `cleric`, `merchant`.
- Supports branching progression and multi-step rank journeys.
- Requirement system supports item, currency, experience level, permission, location, playtime, composite, choice, skills/jobs, and plugin-based requirements.
- Reward system supports item, currency, experience, command, composite, choice, permission, and perk rewards.
- LuckPerms integration can be used for rank-to-group assignment workflows.
- Rank views include tree navigation, requirement detail, and reward-focused progression UI.

For RPG, MMO, survival, and network servers, RDQ ranks create clear goals and strong progression pacing.

### 4) Bounties

Bounties add competitive risk and emergent player-versus-player economy loops.

- `/rq bounties` route is reserved and permission-gated with raindropquests.command.bounties
- Players can place bounties on targets with configurable guardrails.
- Claim mode options include `LAST_HIT`, `MOST_DAMAGE`, and `DAMAGE_SPLIT`.
- Distribution modes include `INSTANT`, `DROP`, `CHEST`, and `VIRTUAL`.
- Configurable tax rate, expiration, tracking windows, announcement scope, and active bounty limits.
- Optional visual indicators for bounty targets.
- Premium mode supports persistent bounty data and advanced hunter progression.

Bounties are excellent for creating high-stakes moments, conflict hotspots, and naturally generated server stories.

## Platform and Integrations

- Website and ecosystem: `https://raindropcentral.com`
- Supported server stacks: Bukkit/Spigot/Paper/Folia
- Optional integrations detected/used by RDQ: economy providers, permission providers, placeholder expansion flows, and skills/jobs ecosystems
  - JExEconomy
  - PlaceholderAPI
  - mcMMO
  - AuraSkills
  - JobsReborn
  - EcoJobs
  - Vault
  - TheNewEconomy

## Setup

### Requirements

- Java `21+`
- Minecraft API `1.19+` (Spigot/Paper/Folia target)
- Optional: Vault, LuckPerms, PlaceholderAPI, supported skills/jobs plugins

### Installation

1. Build or obtain your preferred RDQ edition jar.
2. Place exactly one edition jar in `plugins/`.
3. Start the server to generate default RDQ configs.
4. Configure RDQ in `plugins/RDQ/`.
5. Restart or reload your server process after major configuration updates.
6. Use the in-game admin setup tools with `/rq admin`

### Key Configuration Files

- `plugins/RDQ/database/hibernate.properties`
- `plugins/RDQ/ranks/rank-system.yml`
- `plugins/RDQ/ranks/paths/*.yml`
- `plugins/RDQ/bounty/bounty.yml`
- `plugins/RDQ/perks/perk-system.yml`
- `plugins/RDQ/perks/*.yml`
- `plugins/RDQ/permissions/permissions.yml`

### Database Support

RDQ supports embedded and external database setups through database properties configuration:

- H2 (default embedded option)
- MySQL
- MariaDB
- PostgreSQL
- Oracle
- Microsoft SQL Server
- SQLite
- HSQLDB

## Commands

Primary command registration: `/prq`  
Primary alias: `/rq`

Usage:

```text
/rq <admin | bounty | main | perks | quests | ranks | scoreboard>
```

| Command | Description | Permission |
|---|---|---|
| `/rq` | Shows help when no subcommand is provided | Any accessible `raindropquests.command.*` route |
| `/rq main` | Opens main overview menu | `raindropquests.command.main` |
| `/rq bounty` | Opens bounty system menu | `raindropquests.command.bounty` |
| `/rq perks` | Opens perk overview view | `raindropquests.command.perks` |
| `/rq ranks` | Opens rank overview view | `raindropquests.command.ranks` |
| `/rq quests` | Reserved quest route (in development) | `raindropquests.command.quests` |
| `/rq admin` | Opens admin overview (LuckPerms-aware flow) | `raindropquests.command.admin` |
| `/rq scoreboard perks` | Toggles perks sidebar scoreboard | `raindropquests.command.scoreboard` |

## Permissions

RDQ command routing currently uses `raindropquests.command.*` nodes.

| Node | Purpose                                      |
|---|----------------------------------------------|
| `raindropquests.player` | Parent perm providing all player permissions |
| `raindropquests.command` | Root command node                            |
| `raindropquests.command.admin` | Access admin route                           |
| `raindropquests.command.bounty` | Access bounty route                          |
| `raindropquests.command.main` | Access main route                            |
| `raindropquests.command.perks` | Access perks route                           |
| `raindropquests.command.quests` | Access quests route                          |
| `raindropquests.command.ranks` | Access ranks route                           |
| `raindropquests.command.scoreboard` | Access scoreboard route                      |

Note: `permissions/permissions.yml` also contains larger permission template sets (including `rdq.*` matrices) for admin assignment workflows using `/rq admin` and going to permissions menu.

## Free vs Premium Comparison

| Capability | Free Edition                  | Premium Edition                             |
|---|-------------------------------|---------------------------------------------|
| Rank trees a player can fully progress | `1`                           | Unlimited                                   |
| Ranks per tree that can be progressed | `3`                           | Unlimited                                   |
| Active rank paths | `1`                           | Unlimited                                   |
| Non-selected trees/ranks in UI when limits are reached | Preview-only behavior         | Fully accessible                            |
| Bounties per commissioner | Fixed max `1`                 | Unlimited - Config-driven (`bounty.yml`)    |
| Rewards per bounty | Fixed max `1`                 | Unlimited - Config-driven (`bounty.yml`)    |
| Hunter progression and top hunters | Minimal/free-limited behavior | Full hunter stats and leaderboard workflows |
| Perk framework | Included                      | Included                                    |
| Perk active-slot cap | `5`                           | Configurable via `maxEnabledPerksPerPlayer` |
| Quests system | In development                | In development                              |

## SEO Search Targeting

RDQ is positioned for these search intents:

- Minecraft quests plugin
- Minecraft rank progression plugin
- Minecraft perks plugin
- Minecraft bounty plugin
- Minecraft RPG progression plugin
- Paper quests plugin
- Spigot quest and rank plugin
- Folia compatible progression plugin
- Minecraft LuckPerms rank integration plugin
- Minecraft bounty leaderboard plugin
- Minecraft configurable perk system
- Minecraft GUI progression plugin
- Minecraft plugin with quests ranks perks bounties

## Conclusion

RDQ gives your server a complete progression stack with marketing-friendly depth and player-facing clarity.  
Launch with ranks, perks, and bounties today, then scale into quests as development continues, all inside the RaindropCentral ecosystem.
