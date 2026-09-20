EnthusiaTags Advancement Rewards Test 2
========================================

Test plugin:
- EnthusiaTags-2.2.2-rewards-gui-test.2.jar
- SHA-256: BC6CAE86ED388F369C043758571053B64FE8895B1FA526532DDCAD1BB1F1FF85

Matching renderer:
- EnthusiaAdvancements-1.0.0-pilot.5.jar
- SHA-256: CAB9D863AF9BA945F11B12FACDB6C3BD85CAFD4F86828D91C66FDFF753B1C465

Adds 38 claimable advancement rewards to /rewards:
- WarzoneDuels: 9
- EnthusiaCommend: 10
- EnthusiaExpress: 11
- DiaryKeeper: 8
- 12 advancement-earned tags
- 16,850 total Raw Gold across all advancement payouts

Existing config.yml and rewards.yml version 5 are migrated to version 6.
Backups are created before migration. Existing administrator entries with the
same advancement reward/tag IDs are preserved rather than overwritten.

Negative-reputation milestones Bad Reputation and Public Enemy award tags only;
they intentionally grant no Raw Gold.

Stop the TEST server before replacing jars. Keep the existing EnthusiaTags data
folder. Use only one jar per plugin. No PR, merge, release, or production
deployment is represented by these binaries.
