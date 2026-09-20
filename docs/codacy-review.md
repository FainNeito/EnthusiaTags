# Codacy review pass

## Scope and evidence

The initial API snapshot identified 113 new findings on foundation PR #1 at c9147a6, 18 on Warzone PR #2, and one each on PRs #3 to #5. Counts describe the analyzer report, not confirmed exploitable defects. Full issue IDs and original source locations were saved in the local codacy-findings snapshots before edits. Codacy settings, quality thresholds, branch protections, and production installations remain unchanged.

## Foundation changes

- Companion metadata now uses a hash-pinned defusedxml parser that rejects DTDs, entities, external references, oversized input and workspace escapes. Python validates exact companion repositories, full commit hashes, the known presence-contract path, version agreement and content digest. A fixed shell script performs the Git, Maven and HTTPS download steps only after validation; it does not execute a command supplied by metadata.
- EARS response validation is linear string processing rather than an ambiguous regex, and evidence parsing is split into small pure functions without relaxing required task evidence or phase guards.
- The display tick and gold-reservation transaction are decomposed for reviewability. The gold transaction still obtains its writer lock before reading ownership, retains terminal withholding, and rolls back on failures. No new reward or network bypass is introduced.
- Tests now use real constructor/storage seams, public lifecycle methods and scoped supported mocks instead of Unsafe allocation or private visibility changes. The playtime parser is a package-local pure unit shared by production and regression tests.
- Reported documentation formatting and import issues are corrected without changing advancement requirements.

## Context-sensitive audit findings

Dynamic-filename findings in the SPEAR test fixture and local command-line tools need source-context review. These scripts are not shipped as player-facing server APIs. Test paths originate from a newly allocated private temporary directory; CLI paths are explicit operator inputs under that operator's filesystem permissions. In particular, a numeric file descriptor used by writeFileSync is not a path traversal input. No rule or file is disabled, and no blanket suppression is added in this pass. Any individual false-positive disposition must retain its exact issue ID and rationale.

## Validation

The Java foundation clean verify passed 146 tests with zero failures/errors/skips after the refactor. Eight new Python bootstrap regressions cover digest/version mismatch, malicious metadata, XML entities, size limits and path escapes. The final Node run passed 12 tests with zero failures/skips and EARS validation passed. Python passed all eight tests. The Linux shell orchestration is tested by the actual hosted job, not claimed as locally executed on this Windows checkout. A local build does not imply Codacy approval.

## Warzone follow-on

The Warzone parser has been decomposed without relaxing malformed-evidence rejection. Its overlapping-refresh test now blocks a real injected reader rather than modifying a private AtomicBoolean. Implicit-counter regressions use real storage hydration and public progress reads rather than Unsafe or private field access. Clean Java 25 verification passed 176 tests, zero failures/errors/skips; inherited Node and Python gates are run separately. The original nine advancement keys, event counters, opt-in flag and no-reward behavior are unchanged.
