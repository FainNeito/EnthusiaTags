ENTHUSIATAGS REWARDS GUI — TEST BUILD 1
Version: 2.2.2-rewards-gui-test.1
Based on: a6903229bd3af75f416fcfdd060f968311e5ab95, with GUI-only working-copy changes.

INSTALL ON THE TEST SERVER
1. Stop the server fully. Back up its existing EnthusiaTags JAR and data folder.
2. Replace the existing EnthusiaTags JAR with EnthusiaTags-2.2.2-rewards-gui-test.1.jar.
   Keep exactly one EnthusiaTags JAR in plugins/.
3. If the server uses the native Enthusia advancement tab and has an older renderer,
   replace its EnthusiaAdvancements JAR with the included EnthusiaAdvancements-1.0.0-pilot.5.jar.
   Keep exactly one renderer JAR. This companion is an existing matching build, not a GUI rewrite.
4. Keep plugins/EnthusiaTags/ including rewards.yml, config.yml, all databases and player progress.
   No config deletion/regeneration or reward migration is needed for the GUI change.
5. Start the server normally on its existing Paper 26.2 / Java 25 setup; run /rewards.
   Do not use /reload to swap plugins. Other provider plugins remain as installed.

FEATURES
Five-row dashboard; six-row browser with category shortcuts, 21 rewards per page,
filters (All / Ready / Unclaimed / Claimed), progression/name/closest sorting,
Playtime groups, live counters, readable non-italic lore and hour/minute progress.
Ready-to-Claim view spans categories, while every reward is claimed individually.
Reward slots stay stable during live refresh and after claiming; use the summary
or page indicator to refresh the list. Normal clicks do not close the browser.
No new resource pack, Nexo assets, custom icon or claim-all feature is required.

SAFETY AND VERIFICATION
Full clean Maven verification: 255 tests; 0 failures, 0 errors, 0 skipped.
GUI-specific subset: 22 tests; 0 failures/errors/skips.
Final artifact SQLite probe: SHADED_SQLITE_READ_ONLY_OK.
Tests cover layout/pagination, unavailable progress, non-italic lore, glint states,
filters/groups, unsafe click cancellation, repeated clicks, asynchronous failures,
disconnection and not reopening a closed/different menu after claim completion.
Server APIs are mocked for GUI unit tests. This has not been live-tested in-game;
visual appearance and integration behavior still need test-server acceptance.
Reward IDs, criteria, action definitions, gold/IP policy and existing claim path
remain unchanged by the GUI patch. This build includes the existing advancement
integrations from the base commit. No server deployment or PR was performed.

ROLLBACK
Stop the test server, restore the previous plugin JAR(s), retain the data folder,
and restart. If claims are made during testing, those normal claims remain saved;
replacing a JAR does not undo them.
