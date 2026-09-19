# SPEAR tasks

## T-006 [TDD] Review reward and shutdown safety

Status: [x] complete locally through SPEAR refine; server staging remains outstanding.
References: REQ-016, REQ-017; implementation.md Persistence and Presentation.
Evidence:
- RewardService.claimInternal loads the ledger before delivery but only checks settled fingerprint conflicts inside the action loop. RewardStorage.reserveGoldActionNow rejects mismatches with SQLException; no schema change is needed.
- NativeAdvancementController.close calls the provider before clearing state; EnthusiaTagsPlugin.onDisable calls this before other services. Retain at-most-once toast policy unchanged.
- Existing RewardGoldNetworkTest and PresenceLifecycleTest establish org.junit.jupiter.api.Test, org.junit.jupiter.api.io.TempDir, org.junit.jupiter.api.Assertions, sun.misc.Unsafe and reflection isolation conventions. Existing Paper dependency supplies org.bukkit.Bukkit, org.bukkit.Server, org.bukkit.plugin.PluginManager, org.bukkit.plugin.java.JavaPlugin; existing pilot supplies io.github.badgersmc.advancements.pilot.ProjectionService. No new runtime dependencies or domain imports.
- Red: ../../review-safety-red.log reports the expected GOLD_VERIFICATION_UNAVAILABLE instead of reconciliation and an escaping provider exception (11 tests, 2 failures, no errors). Green: ../../review-safety-green.log passes all 11 focused tests. Full refine: ../../review-safety-full.log passes 140 Tags-only tests (later than T-005; not the three-project aggregate) with zero failures/errors/skips, including LayerRulesTest. JDK 25, offline cached dependencies and temporary Q: path alias; no production data accessed.
- No domain changes or new implementation imports. Regression fixtures verify persistence, no gold reservation or earlier component ledger mutation, original fingerprint preservation, cleanup and repeat-close behavior. Other review findings and the original test bundle are unchanged by this task.

## T-001 [TDD] Component-scoped gold reservations and durable withholding

Status: complete locally through SPEAR refine. Live staging is not yet performed.
References: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006; implementation.md Persistence.
Evidence:

- Baseline: Maven `test`, 107 tests, zero failures/errors; `../../tags-baseline.log`.
- Prove: `RewardGoldNetworkTest.missingAddressCannotAuthorizeCurrency` failed because no SQLException was thrown; `../../tags-gold-red.log` (behavioral red, not a compilation failure).
- Engine: 117 tests, zero failures/errors including nine network tests and one architecture test; `../../tags-gold-green.log`.
- New pure `GoldRewardPolicy` has no framework dependencies. Infrastructure imports the existing `RewardAction`, `RewardStatus` and storage APIs; tests use existing JUnit Jupiter/SQLite dependencies from pom.xml, no new third-party dependency.
- `RewardStorage.reserveGoldActionNow`: a SQLite writer lock plus a `(reward_id, ip_address)` uniqueness constraint; all gold components share one account owner. Atomic denial and action history; old reservation table is read-only in the new claim path.
- `RewardService.claimInternal`: non-gold components bypass the network gate; withheld is terminal, unavailable verification is retryable; no login-address backfill. Existing unlock and provider evaluation code is unchanged in this slice.
- Reviewed recovery: withheld components count as settled for finalization but never as deposited; pending/uncertain actions retain existing staff-review safeguards. Previously queued legacy items remain accepted obligations, not replayed claims. Existing daily rewards are untouched.
- Test boundary: SQLite/restart/concurrent connections and private claim orchestration exercised locally; real TagService/Vault inventory delivery, packet rendering and server integration still require staging.
- Refine: Maven `clean verify` succeeded with 117 tests, zero failures/errors; `../../tags-gold-verify.log`. EARS validation and `git diff --check` passed. Generated dependency-reduced-pom.xml was restored to its original tracked content; no commits were made.

## T-002 [TDD] Historical-safe native advancement projection

Status: decomposed into T-002a and T-002b; native dependency now downloaded and verified under T-002-deps.
References: REQ-007, REQ-008, REQ-009, REQ-010, REQ-014, REQ-015; implementation.md Presentation.
Evidence:

## T-002a [TDD] Preserve progress on unavailable PlayTime reads

Status: complete locally through SPEAR refine; no live-server verification.
References: REQ-010; implementation.md Presentation.
Evidence:

- `rewards/PlaytimeHook.java`: currently converts missing provider, empty Optional and reflection failures into zero.
- Inspected PlayTimePlugin `api/PlaytimeService.java` and `api/impl/PlaytimeServiceImpl.java` in the existing prototype checkout: `getLifetime(UUID)` returns Optional, including empty when runtime is unavailable; empty cannot establish authoritative zero.
- `rewards/RewardService.java`: `getProgress`, `evaluate`, `isComplete` and `ProgressSnapshot` currently lack per-read availability.
- Existing JUnit dependency supplies `org.junit.jupiter.api.Test` and `org.junit.jupiter.api.Assertions`; Java reflection, collections, UUID and OptionalLong use the standard library.
- New `org.enthusia.tags.advancements.domain.VerifiedProgress` is a pure availability/value decision; infrastructure retains session-local last-good values and marks snapshots unavailable for completion decisions.
- Tests use existing `org.enthusia.tags.PerformanceMonitor` without a live Bukkit server. Behavioral red confirmed missing provider returned zero. Green: 123 tests pass, including provider exception/empty/replacement/negative reads, last-good retention, legitimate zero and malformed placeholder output. Logs: `../../tags-playtime-red.log`, `../../tags-playtime-green.log`.
- Build uses a copy of the existing Maven cache inside this workspace, offline, and a temporary workspace-only Q: alias for Java's Windows real-path issue. No external cache was written and the alias is removed after each build.
- Refine: offline clean verify passed all 123 tests; `../../tags-playtime-verify.log`. Layer scan confirms VerifiedProgress uses only java.util.OptionalLong; existing architecture test passes. No commits.

## T-002b [TDD] Build and reconcile the native tree

Status: complete locally through SPEAR refine; native client/server staging remains required.
References: REQ-007, REQ-008, REQ-009, REQ-014, REQ-015; implementation.md Presentation.
Evidence:

- Target supplied by user: 26.2, later 26.3; no UltimateAdvancementAPI installed.
- Official 2.8.1 release notes confirm 26.2 support: https://www.spigotmc.org/resources/ultimateadvancementapi-1-15-26-2.95585/updates
- Current EnthusiaAdvancements `build.gradle.kts` declares 2.8.0; its startup copies combat, exploration and guild defaults. Pilot must not enable those trees.
- Verified UAA 2.8.1 API and source constructors for AdvancementTab, RootAdvancement, BaseAdvancement and AdvancementDisplay. Both display notification flags must be false during projection. Explicit displayToastToPlayer/getAnnounceMessage are reserved for committed live unlocks. Reward rendering never calls reward commands.
- Existing RewardStorage.markUnlockedNow uses INSERT OR IGNORE without reporting insertion; repeated asynchronous evaluations can therefore notify twice. Test uses org.enthusia.tags.PerformanceMonitor, org.junit.jupiter.api.Test, org.junit.jupiter.api.io.TempDir, org.junit.jupiter.api.Assertions and existing SQLite; Java reflection observes its actual returned result.
- A separate Maven pilot module inside EnthusiaAdvancements avoids enabling unrelated bundled trees or requiring their plugin integrations. It is explicitly a test-server replacement build, not the complete existing multi-tree distribution. Its public projection API accepts definitions/progress only and cannot claim Tags rewards.
- Confirmed Paper 26.2 API coordinates from https://docs.papermc.io/paper/dev/project-setup/ and https://jd.papermc.io/paper/26.2/ ; Tags and pilot now compile against io.papermc.paper:paper-api:26.2.build.124-stable. UAA AdvancementUtils.SHOW_ADVANCEMENT_MESSAGES_GAMERULE avoids hard-coding the old renamed gamerule field.
- Prove: first insertion returned null instead of true; `../../tags-native-red.log`. Green: actual SQLite insert winner, concurrent connections and restart; history does not execute a claim. CompletionBaseline tests cover silent first complete observation, provider unavailability and verified incomplete-to-complete transitions.
- Pilot dependencies/import evidence from actual 2.8.1 JAR and sources: com.fren_gor.ultimateAdvancementAPI.AdvancementTab, com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI, com.fren_gor.ultimateAdvancementAPI.advancement.Advancement, com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement, com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement, com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay, com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType. UAA schedules/coalesces tab updates; native criteria are limited to 100 percentage steps, never rounding incomplete progress up to completion.
- Paper API adapter imports: org.bukkit.Bukkit, org.bukkit.ChatColor, org.bukkit.Material, org.bukkit.entity.Player, org.bukkit.event.EventHandler, org.bukkit.event.HandlerList, org.bukkit.event.Listener, org.bukkit.event.player.PlayerQuitEvent, org.bukkit.inventory.ItemStack, org.bukkit.plugin.Plugin, org.bukkit.plugin.ServicePriority, org.bukkit.plugin.java.JavaPlugin, org.bukkit.scheduler.BukkitTask. Existing source contracts: org.enthusia.tags.rewards.RewardAction, org.enthusia.tags.rewards.RewardActionType, org.enthusia.tags.rewards.RewardDefinition, org.enthusia.tags.rewards.RewardCriterion, org.enthusia.tags.rewards.RewardCriterionType, org.enthusia.tags.rewards.RewardService. New io.github.badgersmc.advancements.pilot.ProjectionService is provided, never shaded into Tags. org.enthusia.tags.advancements.domain.CompletionBaseline uses only java.util.HashSet and java.util.Set; all remaining new java.* imports are standard library.
- Tags clean verify: 138 tests pass; pilot clean install: 3 tests pass. Logs `../../tags-native-final-verify.log` and `../../ea-pilot-26-verify.log`. Java 25 in-process javac hit its Windows path/cache failure; forked javac through a temporary workspace-only Q: alias succeeded and the alias was removed. No fake red was recorded for that tooling failure.
- Presentation behavior: one Enthusia root and category rows containing only existing definitions; stable encoded IDs; requirement thresholds/reward quantities/manual claim note; silent projection; committed live unlocks celebrate explicitly. Failed reads are omitted, historical claims/unlocks/partial delivery remain complete, reconnect callbacks are fenced by state identity. UAA shared teams are not projected to prevent personal progress crossing accounts. At-most-once notification intent: abrupt stop after durable unlock but before toast may lose that toast; restart must not replay it or any reward.
- Test boundary: real server/client packets, pack rendering, 26.2 startup with the complete plugin set and live RoseChat audiences remain staging checks. No production deployment or 26.3 compatibility claim.
- Refine rerun: `../../tags-native-refine.log`, 138 tests passing, architecture scan green. No commit step, per user instruction.

## T-002-deps [INFRA] Verify the 26.2 advancement dependency

Status: complete locally through SPEAR refine; native integration remains T-002b.
References: REQ-007, REQ-009, REQ-014; implementation.md Presentation and Verification and rollout.
Evidence:

- Official Maven coordinates and source archive: https://nexus.frengor.com/repository/public/com/frengor/ultimateadvancementapi/2.8.1/ ; local `../uaa-2.8.1.jar` and `../uaa-2.8.1-sources.jar`.
- Official runtime distribution metadata: https://api.modrinth.com/v2/version/uSaZfvN8 . Its supported game versions explicitly include 26.2, not 26.3. Downloaded `../UltimateAdvancementAPI-Plugin-2.8.1.jar` and verified its SHA512 against the publisher metadata: `949fae68b88835c099298cd39421204fccd40c58292b5662a8ca5616f233a2f8b1b5f3da48204f927fffe9c7be936042cd438921042e52575148d5d23728c894`.
- Inspected runtime plugin.yml: UltimateAdvancementAPI 2.8.1, main com.fren_gor.ultimateAdvancementAPI.AdvancementPlugin. The Maven API artifact is not substituted for this installable plugin distribution.
- Source `com/fren_gor/ultimateAdvancementAPI/advancement/Advancement.java`: setProgression(..., false) still invokes onGrant; false suppresses rewards only. onGrant separately broadcasts and schedules the toast. T-002b must explicitly suppress historical notifications rather than rely on that boolean.
- Local EnthusiaAdvancements build.gradle.kts compileOnly and testImplementation pins updated together from 2.8.0 to 2.8.1. No runtime logic, new trees, imports or domain dependencies added by this infrastructure slice. Full EnthusiaAdvancements build and native client behavior are not verified by this task.
- Refine: EARS and both repository diff checks passed. Tags full Maven test suite remains green: 126 tests, zero failures/errors/skips, `../../tags-uaa-dependency-verify.log`. No commits or production changes.

## T-003 [TDD] Message selection and RoseChat replacement integration

Status: decomposed into T-003a (Original choice) and T-003b (per-viewer RoseChat replacement).
References: REQ-001, REQ-011, REQ-012, REQ-013; implementation.md Presence integration.
Evidence:

## T-003a [TDD] Explicit Original choices for existing message categories

Status: complete locally through SPEAR refine; live menu and RoseChat behavior still require staging.
References: REQ-001, REQ-011, REQ-012; implementation.md Presence integration.
Evidence:

- `cosmetics/CosmeticsService.java`, `CosmeticType.java`, `CosmeticsMenu.java`: existing independent category selections and configurable templates; deselection already abstains from message replacement.
- `ConfigMigrator.java`: copyMissing merges new bundled entries without replacing existing entries; a cosmetics config-version increment triggers backup.
- `cosmetics/CosmeticsStorage.java`: existing per-player/category selection storage. No schema change required.
- Test dependencies: `org.bukkit.configuration.file.YamlConfiguration`, `org.junit.jupiter.api.Test`, `org.junit.jupiter.api.Assertions` already in pom.xml. `CosmeticType`, `CosmeticDefinition` and `CosmeticsService` remain infrastructure; no new domain dependencies.
- Red: bundled configuration lacked an Original choice (`expected join but was null`), `../../tags-original-red.log`. Green: 126 passing tests, `../../tags-original-green.log`; Original ignores even a mistakenly configured message, remains first after migration, and is independently configured for join/quit/kill_message.
- Refine: offline clean verify passed all 126 tests; `../../tags-original-verify.log`. Existing architecture suite remains green, no new third-party imports, no changes to player selections or reward definitions. No commits.

## T-003b [TDD] Safe RoseChat presence replacement

Status: complete locally through SPEAR refine; paired-server staging is still required.
References: REQ-011, REQ-012, REQ-013; implementation.md Presence integration.
Evidence:

- Inspected sibling RoseChat PlayerListener: join at NORMAL and quit at HIGHEST directly send per-viewer messages after staff visibility checks. Tags must not re-broadcast through Bukkit; replacement requires an explicit hook inside that delivery path.
- Confirmed checkout origin https://github.com/wsg138/Enthusia-RoseChat.git. Existing org.bukkit.entity.Player, org.bukkit.event.EventHandler, org.bukkit.event.EventPriority, org.bukkit.event.player.PlayerQuitEvent and org.bukkit.event.entity.PlayerDeathEvent are supplied by the current Paper API. Test uses org.junit.jupiter.api.Test and org.junit.jupiter.api.Assertions, standard Java reflection/collections and sun.misc.Unsafe for constructor-free listener isolation, not runtime code.
- New public RoseChat PresenceMessageEvent is an infrastructure contract; Tags will bind only this explicit event through the RoseChat plugin classloader, preserving optional dependency loading. No reflective access to RoseChat private internals and no domain framework dependencies.
- Imports/evidence: org.bukkit.event.Event, org.bukkit.event.Cancellable, org.bukkit.event.HandlerList, org.bukkit.event.EventException, org.bukkit.event.Listener, org.bukkit.plugin.Plugin and org.bukkit.plugin.java.JavaPlugin use the existing Paper dependency. dev.rosewood.rosechat.api.event.PresenceMessageEvent is the new companion API. org.junit.jupiter.api.io.TempDir and javax.tools.ToolProvider compile and exercise that actual companion source in the Tags integration test. org.enthusia.tags.cosmetics.RoseChatPresenceHook is optional infrastructure wiring.
- Behavioral red: HIGH quit handler removed the selection before RoseChat HIGHEST delivery (`tags-presence-red.log`). Green tests cover retained selection then MONITOR cleanup, RoseChat ownership, suppressed Bukkit messages, standalone fallback, actual event replacement/cancellation and Original preservation. RoseChat clean build succeeded (`../../rosechat-presence-verify.log`); no runtime client test yet.
- Refine: Tags clean verify passed 130 tests, zero failures/errors/skips (`../../tags-presence-verify.log`); companion RoseChat clean build passed 12 tests, zero failures/errors. No new domain code; integration remains infrastructure-only. No existing cosmetic selections or challenge definitions changed. Generated dependency-reduced-pom.xml restored after packaging. No commits.

## T-004 [INFRA] Package the logo and local pilot artifacts

Status: complete locally through SPEAR refine; test-server artifacts assembled, not deployed.
References: REQ-007, REQ-014; implementation.md Verification and rollout.
Evidence:

- Minecraft 26.2 release notes specify resource-pack format 88.0: https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2 . Modern min_format/max_format schema is described at https://www.minecraft.net/en-us/article/minecraft-snapshot-25w31a . The supplied PNG is copied unchanged, not regenerated.
- Native root icon uses PAPER with custom-model-data 815001. Pack range dispatch maps that value to enthusia:item/logo and restores normal paper at 815002; no pack means ordinary vanilla paper. Existing server packs must merge the paper selector rather than overwrite their own mappings.
- Bundle contains local Tags, projection-only EnthusiaAdvancements, companion RoseChat, official checksum-verified UAA 2.8.1, optional logo pack, install checklist and checksums. No production files are read or changed.
- Final clean builds: `../../tags-pilot-package.log` (138 tests), `../../ea-pilot-package.log` (3 tests), `../../rosechat-pilot-package.log` (12 tests). Resource JSON parses, archive metadata is at root, PNG checksum matches the supplied original, plugin descriptors are inspected, and Tags contains no shaded UAA/pilot API classes. Pilot and Tags descriptors explicitly target api-version 26.2. All artifacts are under `../../outputs/EnthusiaTags-26.2-test-bundle`.
## T-005 [DOC] Operator migration, compatibility and staging checklist

Status: complete locally through SPEAR refine; ready for user-run test-server staging.
References: REQ-001 through REQ-015; implementation.md Verification and rollout.
Evidence:

- `../../outputs/EnthusiaTags-26.2-test-bundle/TEST-SERVER-README.md` documents all four JARs, full-stop test installation, isolated database backends, old binary rollback restrictions, resource-pack merge, individual UAA teams, manual claims, historical/live behavior and a fresh-player threshold test using an existing challenge.
- Exact staged plugin descriptors and publisher checksum were inspected in T-004. Latest clean tests total 153 across Tags (138), pilot renderer (3) and RoseChat (12). No real-server/client or 26.3 validation is implied. Full Kotlin/Nexus EnthusiaAdvancements is explicitly distinguished from the isolated pilot module.
- SPEAR was followed for each implementation slice. Per user instruction, no commit/push/PR/release/deployment step is performed.
- Final cross-check: Tags full test suite rerun (`../../tags-docs-final-test.log`) passed all 138 tests. Final ZIP entries were checked against every SHA256 manifest entry; all matched. Generated Maven reduced POM was restored to its pre-build tracked content. Remaining checks are explicitly user-run server/client staging, not missing implementation tasks.

## T-900 [TDD] PR-review correctness and verification cleanup

Status: locally verified; hosted validation pending the updated head.
References: REQ-901 through REQ-905; implementation.md Presentation, Presence integration, Verification.
Evidence:
- Rechecked original CodeRabbit findings against the foundation branch rather than assuming later branches contain the fixes.
- ReviewRegressionTest initially failed all three cases: repeated hour/minute tokens, combined seconds, and a consumed completion transition. The implementation now passes them; NotificationRetryTest separately verifies failed-delivery retention and persistence retry classification.
- RoseChatLateBindingTest exercises late enable, duplicate enable, and disable/re-enable using the actual event-registration boundary. Original selection is now explicitly selected in the companion-source test. RoseChat presence ownership continues to suppress Bukkit fallback whenever RoseChat is enabled, because broadcasting a fallback without the per-viewer hook would bypass its audience rules.
- Renderer API now requires the registered plugin owner. Bootstrap pins the renderer commit/version and checksums the exact RoseChat API source.
- Eight Node tooling regression tests cover malformed/missing clauses, inline requirements, state shape, shared-lock exclusion, evidence gating, failed atomic rename, concurrent transitions, and Bash delegation. Regression failures were reproduced against the original helpers; the final run passes eight tests.
- Java 25 clean verify passes 146 Tags tests (zero failures/errors/skips), separately from seven companion-renderer tests and eight Node tests. Generated reduced POM is excluded from this change.
- Hosted CI checks the exact head, builds pinned companions, and keeps Codacy as a separate strict gate. Absence of a Codacy result remains a failure, not a skipped approval.
