# Active playtime policy: REQ-914/REQ-915, T-914

## Spec and prove

Isolated branch from canonical wsg138/EnthusiaTags main c0d9162; remote main rechecked before delivery. Original checkout's unrelated edits and prior inactive test artifact preserved. Canonical config schema is 5, while pilots can already contain schemas 7/8, so migration is idempotent and independent of version upgrades.

Observed behavioral red on canonical code: ActivePlaytimeConfigTest ran two tests with two assertion failures. Bundled first_hour still used total minutes, and existing schema-8 time criteria remained total. Dependency bootstrap failures were resolved before this test and are not counted as regression proof.

## Engine and architecture

Bundled first_hour, payday and market_access use PLAYTIME_ACTIVE_MINUTES. ConfigMigrator converts existing/custom total or AFK criteria case-insensitively at startup, preserves thresholds, payouts, custom wording and non-time criteria, and refreshes only exact former default descriptions. Earned/claimed records are not touched. Migration retains higher schemas and reuses the existing file backup mechanism before mutation. Repeated migration is a no-op. No new dependency, permission or runtime call; existing active-minute evaluation remains authoritative.

## Refine and delivery gates

- Java 25 canonical Maven clean verify: 250 tests, zero failures/errors/skips.
- EARS requirements validator passed; SPEAR tooling tests: 12 passed. Recorded spec/prove red/engine green/arch/refine with the existing state helper.
- Companion bootstrap validator: 8 Python tests passed. Exact renderer pin 139177ccba8f37b2d568f2e4d8fb7e5e0b864db8 was built and installed; checksum-pinned RoseChat event source verified. LoreItems 1.0.0 release SHA-256 verified against POM before install. Windows PowerShell executed the same validated fetch/build/checksum steps because Bash was unavailable locally; hosted CI runs canonical Bash scripts.
- Final shaded SQLite artifact probe passed (SHADED_SQLITE_READ_ONLY_OK); git diff checks passed.
- Hosted exact-head verify and Sentinel artifact jobs passed for the initial PR head. Codacy reported one new conditional-literal finding; replaced it with a named resource constant and reran canonical clean verify (250 tests passed). Final-head hosted checks are rechecked after this refinement.
- Network monorepo currently pins Tags to 36bd6c5 and Playtime to 2a5b57d. Component merge, exact merged-commit build, a network pin PR and verified combined build are required before production delivery.
- No production or staging changes in this follow-up. Local tests do not establish player/client acceptance. Hosted checks and review must be inspected at the exact PR head.
