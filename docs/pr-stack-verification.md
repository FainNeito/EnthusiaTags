# Stacked PR verification

## Warzone consumer

Historical foundation-sync verification at df3dd31 (before the malformed-section follow-up): {"tests":173,"failures":0,"errors":0,"skipped":0}. The nine existing duel advancements and keys remain unchanged. Consumer calls now pass the owning plugin; renderer dependency is pinned to pilot.4 at 1cefd9f. No deployed server or player data changed. Eight separate Node tooling tests also run before push.

## Warzone malformed-section follow-up

The recorded red run round2-warzone-shape-red.log reproduced two failures: a scalar/list advancement section was accepted, and could establish a false baseline. After validating section shape, round2-warzone-verify.log completed clean verify with 176 Java tests, zero failures/errors/skips. Legacy absent/empty sections remain accepted, and corrected first snapshots are silent. The accompanying eight-test Node and EARS gates are checked separately. Nine-node documentation and provider requirements are synchronized. This run does not imply live server acceptance or Codacy approval.
