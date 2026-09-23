# Stacked PR verification

## Warzone consumer

Historical foundation-sync verification at df3dd31 (before the malformed-section follow-up): {"tests":173,"failures":0,"errors":0,"skipped":0}. The nine existing duel advancements and keys remain unchanged. Consumer calls now pass the owning plugin; renderer dependency is pinned to pilot.4 at 1cefd9f. No deployed server or player data changed. Eight separate Node tooling tests also run before push.

## Warzone malformed-section follow-up

The recorded red run round2-warzone-shape-red.log reproduced two failures: a scalar/list advancement section was accepted, and could establish a false baseline. After validating section shape, round2-warzone-verify.log completed clean verify with 176 Java tests, zero failures/errors/skips. Legacy absent/empty sections remain accepted, and corrected first snapshots are silent. The accompanying eight-test Node and EARS gates are checked separately. Nine-node documentation and provider requirements are synchronized. This run does not imply live server acceptance or Codacy approval.

## Commendation foundation-sync history

The earlier run at 2f83047 passed 187 Java tests and eight separate Node tests. That count predates the following malformed-section fixes and is not the current validation total.

## Commendation malformed-section follow-up

After the Warzone parser fixes were synchronized, round2-commend-verify.log passed clean verify with 195 Java tests, zero failures/errors/skips. The focused pre-fix run reproduced four failures; the fixed 14-test reader/bridge run passed. Wrong-shaped advancementEvidence and categoryMax fields now reject snapshots, while genuinely missing version-9 data and empty category maps remain compatible. Corrected historical data cannot trigger false live-completion toasts. This records local verification separately from exact-head hosted checks and the still-required Codacy gate.

## Express foundation-sync history

The earlier run at f0a6716 passed 197 Java tests plus eight Node tests. That count precedes the parser follow-up and return-lifecycle contract coverage. The latest completed run is recorded below once verification completes.

## Express provider-contract follow-up

round2-express-verify.log passed clean verify with 207 Java tests, zero failures/errors/skips, including the inherited malformed-evidence fixes and two new return-lifecycle fixtures. Final shading verification passed. The focused Express reader/bridge run passed eight tests. Inspection of the pinned provider confirmed returned rows already change recipient_uuid to the original sender, so aggregation behavior remains unchanged; docs/express-history-contract.md records the source and fixture limits. No mail database or server was modified.
