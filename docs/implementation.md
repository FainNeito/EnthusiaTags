# Implementation and safety boundaries

## Layer Dependency Rules

domain <- application <- infrastructure

New pure policy code belongs in `org.enthusia.tags.advancements.domain` and uses Java standard-library types only. Application orchestration may depend on domain; Bukkit, SQL, provider APIs and the existing legacy reward/cosmetics packages are infrastructure. Legacy packages are not relocated in this migration. New provider adapters must not expose database connections or issue rewards from the rendering layer.

## Forbidden Domain Annotations

```yaml
forbidden: []
```

## Persistence

Only additive schema changes. Preserve reward_claims, reward_unlocks and existing action IDs/fingerprints. Gold ownership is per challenge and IP (one account owns all its gold components), atomically serialized by the storage executor and protected by a database uniqueness constraint. Delivery and withholding remain per action. Legacy whole-challenge IP reservations remain read-only evidence for the new claim path. A terminal withheld component records an explicit reason rather than pretending money was deposited. Failed verification must not become permanent withheld status. Gold limits must cover retries, overflow delivery and admin recovery, not merely the first click.

## Presentation

EnthusiaTags remains authoritative. Native advancement progress is a rebuildable projection. Historical reconciliation is silent; live completion is celebrated once. Native advancement criteria do not invoke reward commands. Existing claim UI remains accessible because vanilla advancement clicks are not an ordinary server-side claim interface. Missing dependencies disable the projection without disabling claims.

## Presence integration

Use a supported per-viewer integration point after RoseChat visibility filtering and before original message delivery. Original means abstaining from replacement, not reconstructing a guessed default. Preserve selections until quit message delivery has completed. Never broadcast a replacement outside the owner plugin's audience.

## Warzone Duels statistics bridge

The opt-in bridge reads the enabled WarzoneDuels plugin's `stats.yml` off-thread every five seconds. It never writes that file or accesses Tags reward tables. A strict parser requires explicit nonnegative wins and best-win-streak fields per UUID; a failed snapshot is omitted, not zero. Absence of a UUID in a valid complete snapshot proves zero history; absence of the file does not. Initial observations (including after reconnect/restart) wait for a read started after joining and are silent; later threshold crossings celebrate once per online session. Known progress never regresses during a session. An interrupted server may lose a toast, never replay payments. Existing WarzoneDuels statistics include party victories; these achievements use that same definition of a win, not guild membership. Source support is verified against the local WarzoneDuels 1.0.3 checkout; incompatible schemas fail closed.

Only three statistics-backed milestones ship in this slice. Menu visits, spoils withdrawals, surrender, modifiers, low-health victories, spectator bets and hidden interactions require future event hooks; no historical inference is made for them. No new monetary or cosmetic rewards are configured for these three milestones. Enable/disable requires a plugin/server restart.

## Verification and rollout

SPEAR cycles: spec, prove (behavioral red), engine (green), architecture, refine. Record test commands and results per task. Validate existing databases through temporary SQLite fixtures, concurrency/restart/recovery tests, then produce a local test artifact only after package verification. Staging must check the actual Paper/client versions, logo pack, toast timing, RoseChat visibility and restart behavior. No live approval can be inferred from unit tests.
