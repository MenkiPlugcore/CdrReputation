# CdrReputation

`CdrReputation` is the core reputation service for MoonSign S2. It is intentionally focused on reputation only so other systems such as BetonQuest integrations, quest journals, bounty hunting, NPC dialogue, and future gameplay modules can consume one stable API.

## v0.1.0 — Core & API

Implemented in this release:

- UUID-based reputation storage.
- Configurable minimum, maximum, default value, and reputation tiers.
- SQLite persistence with WAL mode.
- Permanent reputation history/audit log with reason, source, actor, old value, new value, and timestamp.
- In-memory cache for fast reads during gameplay.
- Public Bukkit `ServicesManager` API for other plugins.
- `ReputationChangeEvent` for loosely-coupled integrations.
- Admin-only commands; players do not need reputation commands.
- Java 21 / Paper 1.21.11 target.

## Default tiers

| Tier | Range |
| --- | ---: |
| Revered | 1000 to 5000 |
| Respected | 500 to 999 |
| Trusted | 100 to 499 |
| Neutral | -99 to 99 |
| Suspicious | -499 to -100 |
| Outlaw | -999 to -500 |
| Infamous | -5000 to -1000 |

All tier names and ranges can be changed in `config.yml`.

## Admin commands

Permission: `cdrreputation.admin` (default: OP)

```text
/cdrrep get <player>
/cdrrep set <player> <value> [reason]
/cdrrep add <player> <amount> [reason]
/cdrrep remove <player> <amount> [reason]
/cdrrep history <player> [limit]
/cdrrep reload
```

Player-facing gameplay is expected to be driven by NPCs and other plugins rather than commands.

## API

Other plugins should resolve the registered API instead of accessing the database directly.

```java
CdrReputationApi api = CdrReputationProvider.get();

api.addReputation(
    player.getUniqueId(),
    25,
    ReputationContext.of("Completed Lost Cargo", "BETONQUEST")
);

int reputation = api.getReputation(player.getUniqueId());
ReputationTier tier = api.getTier(player.getUniqueId());
```

A `ReputationChangeEvent` is fired after a successful non-zero change. This is intended for integrations such as CdrBounty, NPC feedback, scoreboards, and achievement systems.

## Storage

The default database is `plugins/CdrReputation/reputation.db`. Tables are created automatically on first start. SQLite JDBC is bundled inside the plugin JAR.

## Build

Requirements: JDK 21 and Maven 3.9+.

```bash
mvn clean package
```

Output: `target/CdrReputation-0.1.0.jar`.

## Planned integrations

- v0.2.0: BetonQuest reputation events/conditions.
- CdrQuestJournal: quest journal, live objective progress, timers, and NPC turn-in.
- CdrBounty: wanted/bounty gameplay backed by the same reputation API.

## License

MENKIESTES SOFTWARE LICENSE v1.0. See [`LICENSE`](LICENSE).
