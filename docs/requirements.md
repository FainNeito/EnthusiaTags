# EnthusiaTags advancement pilot

Users: existing and new Enthusia players, including alternate accounts; server administrators operating existing live databases.

- REQ-001: THE SYSTEM SHALL preserve existing player identifiers, challenge identifiers, earned tags, cosmetic selections, claim history and reward delivery recovery records.
- REQ-002: WHEN a player qualifies for an existing challenge THE SYSTEM SHALL allow completion independently of another account sharing the player's IP address.
- REQ-003: WHEN a player claims a challenge THE SYSTEM SHALL restrict only its Raw Gold components to one account per IP while allowing its other eligible reward components.
- REQ-004: IF a Raw Gold network reservation cannot be verified THEN THE SYSTEM SHALL withhold its delivery without treating the storage failure as an established claim by another account.
- REQ-005: WHEN a component has already been delivered or durably withheld for the network limit THE SYSTEM SHALL avoid replaying that component during retries or recovery.
- REQ-006: THE SYSTEM SHALL preserve legacy IP reservation evidence without manufacturing historical claims from a player's current login address.
- REQ-007: THE SYSTEM SHALL display existing challenges in an Enthusia vanilla advancement tree with requirements, reward descriptions and the supplied logo through a compatible resource-pack item icon.
- REQ-008: WHEN a player newly completes a challenge THE SYSTEM SHALL emit one native advancement toast and advancement-style announcement.
- REQ-009: WHEN historical completion is reconciled THE SYSTEM SHALL restore advancement display silently without granting rewards again.
- REQ-010: IF an authoritative progress provider fails THEN THE SYSTEM SHALL retain known progress and retry rather than substitute zero or infer completion.
- REQ-011: THE SYSTEM SHALL support configurable kill, join and leave message rewards with independent player selections and an Original option.
- REQ-012: WHEN a player selects Original for join or leave messages THE SYSTEM SHALL restore RoseChat's configured default behavior.
- REQ-013: WHEN a selected custom presence message replaces RoseChat's message THE SYSTEM SHALL preserve RoseChat's recipient and visibility restrictions without duplicate delivery.
- REQ-014: THE SYSTEM SHALL keep future plugin advancement providers extensible without adding Guild, Death Duels, Market or other new challenge definitions in this pilot.
- REQ-015: THE SYSTEM SHALL preserve manual claim behavior unless explicitly configured otherwise and distinguish completion from reward delivery.

## Policy interpretation for this pilot

- REQ-016: IF any configured reward action conflicts with its saved fingerprint THEN THE SYSTEM SHALL persist reconciliation-required status before reserving or delivering any component.
- REQ-017: IF the advancement provider throws during tree removal on shutdown THEN THE SYSTEM SHALL log the failure and clear controller state without interrupting remaining plugin shutdown.

The existing MONEY action is the server's Vault-backed Raw Gold currency (see RewardMoneyPolicy). Explicit RAW_GOLD and RAW_GOLD_BLOCK item actions also count as gold. Arbitrary reward commands are not parsed as currency: administrators must use typed gold actions for gold payouts; otherwise commands cannot be safely classified. Existing whole-reward IP reservations are conservative evidence for gold only. No historical rewards are removed or replayed.

A network-limited component is durably withheld for that account; changing IP later must not make it claimable again. Missing IP or database failure is retryable, not a permanent rejection. No implicit account whitelist exception is introduced for gold.

## PR cleanup safety and verification

- REQ-901: WHEN a live completion is observed THE SYSTEM SHALL recognize its transition once while retaining unacknowledged live evidence across retryable persistence failures.
- REQ-902: IF the renderer rejects a pending celebration THEN THE SYSTEM SHALL retain that pending work for a later attempt.
- REQ-903: WHEN RoseChat enables after Tags THE SYSTEM SHALL install its per-viewer presence binding once while preserving RoseChat audience restrictions and defaults when the API is unavailable.
- REQ-904: WHEN repeated playtime unit tokens are parsed THE SYSTEM SHALL count every occurrence with checked arithmetic and combine seconds before rounding.
- REQ-905: WHEN local workflow tooling reads or modifies task state THE SYSTEM SHALL reject malformed state, serialize concurrent operations, enforce required evidence, and replace files atomically.
