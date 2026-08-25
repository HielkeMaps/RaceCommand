# RaceCommand

A Paper plugin that adds player-hosted races to a parkour map. Players create a race, invite friends, and race to the finish with a synchronized countdown, live timing, and placement results — plus two extra game modes: **PvP** and **Infected**.

Built for the [Parkour Spiral](https://hielkemaps.com) maps by Hielke, but usable on any map that follows the small map contract described below.

## Features

- **Player-hosted races** — anyone can create a race, invite others, and start it. Invites are clickable and races can be public or private.
- **Synchronized countdown** — all racers are teleported to the start, made invisible for the last ten seconds, and released together on "GO".
- **Timing & placements** — finish times are read from the map's `time` scoreboard and announced with 1st/2nd/3rd placements.
- **Three race modes**
  - `normal` — a plain race to the finish. Optional *ghost players* (racers can't see or collide with each other).
  - `pvp` — same as normal, but hitting other racers is allowed.
  - `infected` — one player starts as a zombie villager and must infect the others before anyone reaches the finish. Infected players get a hotbar of abilities (leap, speed, blindness, glowing, arrows); villagers win if someone finishes, infected win if everyone is turned.
- **Scheduled events** — a proxy/lobby plugin can create server-wide event races over the `hielkemaps:main` plugin-message channel, with a fixed start time and PlayerPoints prizes for the top finishers.
- **Race owner controls** — kick, disband, force-join (OP), broadcast finishes to the whole server (OP).

## Commands

All commands live under `/race`. Tab-completion only shows the subcommands that make sense for your current state (in a race / owner / race started, etc.).

| Command | Who | Description |
|---|---|---|
| `/race create [normal\|pvp\|infected]` | anyone | Create a race (defaults to `normal`) |
| `/race invite <player>` | owner | Invite a player |
| `/race invite <selector>` | OP | Invite many players at once (e.g. `@a`) |
| `/race join <owner>` | anyone | Join a public race or one you were invited to |
| `/race leave` | member | Leave the race you're in |
| `/race start [countdown]` | owner | Start the race, optional countdown in seconds (3–1000) |
| `/race stop` | owner | Stop a running race |
| `/race info` | member | List racers, finish status and times |
| `/race kick <player>` | owner | Remove a player from your race |
| `/race disband` | owner | Delete the race |
| `/race forcejoin <player>` | OP | Pull a player into your race, even mid-race |
| `/race option visibility <public\|private>` | owner | Let anyone join, or invite-only |
| `/race option ghostPlayers <true\|false>` | owner | Hide racers from each other (not in `infected`) |
| `/race option firstInfected <player>` | owner | Choose who starts infected (`infected` only) |
| `/race option infectedDelay <seconds>` | owner | How long the first infected is frozen (min 5) |
| `/race option broadcast <true\|false>` | OP | Broadcast finishes server-wide |

Races need at least two players to start. OPs can also manage any race they are part of.

## Requirements

- **Paper 1.21.x** (API version 1.21), Java 17+
- Plugin dependencies (hard depends, must be installed):
  - [CommandAPI](https://commandapi.jorel.dev/) — command registration
  - [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81/) — villager/zombie/skeleton disguises in Infected mode (requires [PacketEvents](https://github.com/retrooper/packetevents))
  - [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/) — prize payouts for event races

## Map contract

RaceCommand doesn't implement the parkour itself; it hooks into a map that provides:

| Hook | Used for |
|---|---|
| `/restart` command | Teleports a player to the start (run for every racer at 10 s before start) |
| `function time:start` | Run as each player on "GO" to start their timer |
| `time` and `time_tick` scoreboard objectives | Read for finish times; synced when players join a race mid-way |
| `finished` scoreboard tag on a player | Marks that the player crossed the finish line |
| `inRace` scoreboard tag | Set by the plugin while a player is racing, so map logic can react |

## Installation

1. Install the dependencies listed above.
2. Drop `RaceCommand-<version>.jar` in your server's `plugins/` folder.
3. Restart the server. A `config.yml` is generated on first run.

## Building

This is a standard Maven project:

```bash
mvn package
```

The shaded jar ends up in `target/RaceCommand-<version>.jar`.

> The `pom.xml` also has an `install`-phase step that copies the jar to a local dev server path. Adjust or remove it for your own setup.

## Project layout

```
src/main/java/io/github/hielkemaps/racecommand/
├── Main.java              # Plugin entry point, event-race plugin messaging
├── Commands.java          # All /race subcommands (CommandAPI)
├── race/
│   ├── Race.java          # Base race: countdown, timing, finishing, options
│   ├── RaceManager.java   # Registry of active races and the current event
│   ├── RacePlayer.java    # Per-racer state (finished, infected, points)
│   └── types/             # NormalRace, PvpRace, InfectedRace
├── abilities/             # Infected-mode hotbar abilities
├── events/                # Bukkit event listener
└── wrapper/               # PlayerWrapper/PlayerManager: invites, disguises, abilities
```

## License

[MIT](LICENSE) © Hielke
