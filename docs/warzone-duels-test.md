# Warzone Duels advancement integration

This optional, display-only follow-on adds a row to the existing Enthusia advancement tab:

| Display name | Stable key | Requirement |
| --- | --- | --- |
| Arena Initiate | warzone_duels/first_blood | 1 duel win |
| Arena Win Streak | warzone_duels/unstoppable | Best duel win streak of 5 |
| The Gladiator | warzone_duels/gladiator | 50 duel wins |

The first two display names deliberately differ from existing combat advancements First Blood and Unstoppable. Their internal keys remain stable. Individual and Duel Party wins use WarzoneDuels' own statistics; guild membership is not involved. No additional currency or cosmetic rewards are granted.

## Enabling

Keep the existing EnthusiaAdvancements pilot companion and compatible UltimateAdvancementAPI installation. Back up the current Tags JAR/configuration, stop the server, replace Tags with 2.2.2-pilot.2, and add this key inside the existing `advancements` section:

```yaml
advancements:
  enabled: true
  warzone-duels-enabled: true
```

Do not replace the rest of the configuration or run two Tags JARs. Restart to apply the toggle. WarzoneDuels must be enabled; its JAR does not need replacement for this integration. The flag defaults to false. Disable the flag and restart to remove this optional row.

## Safety and history

The bridge reads WarzoneDuels' `stats.yml` every five seconds off-thread. It requires explicit integer `wins` and `best-win-streak` per player UUID. Missing/unreadable/malformed files are unknown, not zero; already known session progress is retained. A missing UUID in a valid complete snapshot proves no recorded history.

The first fresh post-join snapshot silently restores historical progress. Later observed threshold crossings use the existing native toast/announcement path, once per session. A completion before the initial baseline or during a shutdown may appear silently. No reward ledger is read or written by this bridge, and it never writes duel statistics. Existing reward claims and gold/IP policy are unchanged.

## Verification

- SPEAR red/green tests cover thresholds, historical restoration, failed reads, progress retention, reconnect baselines, fixed keys and configuration wiring.
- Local clean verify: 156 tests passed, zero failures/errors/skips, Java 25 with the pinned Paper 26.2 API.
- Test server: WarzoneDuels 1.0.2 schema inspected; Tags 2.2.2-pilot.2 enabled and registered 104 challenges (101 existing plus 3 duel milestones); startup completed and the duel arena restored.
- User reported the installed result looks good. This is not a claim of exhaustive live threshold/toast, restart, or 26.3 compatibility testing.
- Existing hosting CI still needs the pilot companion dependency and sibling RoseChat contract source; local success is not a hosted-CI approval claim.

Spoils, surrender, modifier/low-health wins, spectator betting and hidden interaction achievements remain future work. No guild advancements or kill-effect changes are included.
