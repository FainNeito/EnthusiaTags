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
- REQ-018: WHEN RoseChat initializes on Paper year-based version strings THE SYSTEM SHALL resolve the Minecraft major and minor versions without interpreting build metadata as a number.
- REQ-019: WHEN existing consecutive-active, underground-active or maximum-ping criteria omit an explicit counter key THE SYSTEM SHALL resolve their established stored counters without modifying challenge thresholds, rewards, claims or administrator-specified keys.
- REQ-020: WHEN RoseChat decorates a clickable message THE SYSTEM SHALL preserve each configured click action on Adventure 4 and 5 runtimes without binary linkage errors.

## Approved follow-on: Warzone Duels statistics slice

REQ-014 remains the original pilot boundary; the user explicitly authorized Warzone Duels on 2026-09-18. Guild and Market advancements remain excluded.

- REQ-021: WHEN the optional Warzone Duels statistics bridge is enabled THE SYSTEM SHALL append Arena Initiate (1 win), Arena Win Streak (best streak 5), and The Gladiator (50 wins) to the existing Enthusia tree using read-only persisted WarzoneDuels statistics without issuing rewards or modifying either plugin's player data.
- REQ-022: WHEN an online player's first valid duel snapshot is observed THE SYSTEM SHALL project historical progress silently and celebrate only subsequent newly observed completions during that session.
- REQ-023: IF duel statistics are missing, unreadable or malformed THEN THE SYSTEM SHALL retain known progress and retry without substituting zero or announcing completion.
- REQ-024: THE SYSTEM SHALL keep the duel bridge disabled by default and perform statistics file reads off the server thread while retaining fixed advancement identifiers and keeping all guild integrations excluded.

The existing MONEY action is the server's Vault-backed Raw Gold currency (see RewardMoneyPolicy). Explicit RAW_GOLD and RAW_GOLD_BLOCK item actions also count as gold. Arbitrary reward commands are not parsed as currency: administrators must use typed gold actions for gold payouts; otherwise commands cannot be safely classified. Existing whole-reward IP reservations are conservative evidence for gold only. No historical rewards are removed or replayed.

A network-limited component is durably withheld for that account; changing IP later must not make it claimable again. Missing IP or database failure is retryable, not a permanent rejection. No implicit account whitelist exception is introduced for gold.
