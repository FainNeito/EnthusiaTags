EnthusiaTags rewards GUI test build
Version: 2.2.2-rewards-gui-test.1
Base source: a6903229bd3af75f416fcfdd060f968311e5ab95 plus local GUI changes.
The GUI source changes are not committed to an existing feature branch or PR.
The download branch contains artifacts only; its source tree is the base, not the GUI source.

Includes: 5-row dashboard; 6-row category browser; 21 rewards per page;
category shortcuts; Playtime groups; filters and sorting; Ready to Claim view;
readable non-italic text, hours/minutes, separate claim states and live refresh.
Claims use the existing service, preserve the open view, and reject repeated clicks.
No Claim All, Nexo requirement, changed payouts, changed reward criteria, or data reset.

Verification: full Java 25 clean verify passed 255 tests, zero failures/errors/skips.
An independent copy also passed 20 focused browser/model/interaction tests.
The final shaded JAR passed an isolated SQLite read-only connection/write-rejection probe.
Live GUI appearance and behavior still require test-server acceptance.

Installation: stop the TEST server and back up the current plugin JARs and data folder.
Replace the existing EnthusiaTags JAR with the supplied GUI test JAR; do not keep both.
Keep your existing config.yml, rewards.yml and databases.
For native advancements, use the included EnthusiaAdvancements 1.0.0-pilot.5 renderer
unless that version is already installed. Replace its older JAR; do not install duplicates.
Keep UltimateAdvancementAPI and your current provider plugins in place.
Start the server normally (not /reload), then open /rewards.
Check category switching, filters, tooltips, completed claims and rapid repeated clicks.
A claim is a real test-server payout, so use a test account/data set for payout tests.

EnthusiaTags SHA-256:
41402850C2FC5E4B067B9DAA54120F2BD8ECB2E983FF59B3C48CCFC891E21462
