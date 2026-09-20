# EnthusiaTags rewards GUI — test build 1

Version: 2.2.2-rewards-gui-test.1. Built from an isolated local snapshot based on a690322; source PRs remain untouched. This branch is used only to host test artifacts. No existing source/PR branch was updated, and no release workflow was run.

## Files

- EnthusiaTags-2.2.2-rewards-gui-test.1.jar: the redesigned rewards browser.
- EnthusiaAdvancements-1.0.0-pilot.5.jar: the existing matching advancement renderer, included because the latest Tags base uses its owner-aware API. Replace older pilot.3/pilot.4 renderer JARs when using this Tags build; do not leave multiple versions installed.
- SHA256SUMS.txt: hashes of the exact tested binaries.

## GUI changes

Five-row dashboard with progress, category cards and Ready to Claim. Six-row browser with category shortcuts, 21 padded reward slots, Playtime grouping, All/Ready/Unclaimed/Claimed filters, Progression/Closest/Name sorting, and conditional pagination. Clear non-italic tooltips, readable time units, distinct claim states and in-place updates. Claims still use the existing RewardService path; rapid duplicate clicks, inventory swaps and foreign inventories are guarded. No Claim All, new advancement rewards, or resource-pack dependency.

## Verification

Final clean Java 25 verification: 255 tests, zero failures/errors/skips, including 36 GUI/model/render/interaction regressions. The isolated final-JAR SQLite check passed SHADED_SQLITE_READ_ONLY_OK against a disposable database. Unit/API-fixture tests are not a live Minecraft visual acceptance test. Reward definitions, configuration, messages and RewardStorage match the a690322 baseline; stored player progress and payout rules were not changed by this redesign.

## Test installation

Stop the test server fully. Back up the old Tags and renderer JARs and the EnthusiaTags data folder. Replace EnthusiaTags with this build and use the included pilot.5 renderer if not already installed. Keep the other provider plugins and existing configuration/data files. Start the server and open /rewards. No Nexo regeneration is needed for this GUI. Do not use a hot-reload tool.

To roll back this GUI test, stop the server and restore the previous Tags/renderer pair. Do not discard player progress or regenerate the rewards configuration as part of rollback.

This test build has not been installed on your server. The custom Diary advancement icon is still deferred. No PRs were created or updated for this build.
