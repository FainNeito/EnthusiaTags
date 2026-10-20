# Express and Pumpkin Patcher source reconciliation

Roundtable task: EXPRESS-07. This records composition of the reviewed unique-mail
change with the newer release-fork main. It does not introduce another mail
policy, reward rule, renderer version or resource-pack design.

## Source receipt

| Input | Exact reference |
| --- | --- |
| Express unique-mail parent, PR #12 | `16a70aaddcea0c29db84b51961a57581e5ca63ac` |
| Release main with merged Pumpkin Patcher PR #13 | `a2d7d9e1f2cd5e5732717a8bc35de56baf1be3da` |
| Merge base | `6bbb2534cef121cce01223da0e536ea338fd6566` |
| Combined index tree before adding this note | `e7342a80c3960242712fc042fac557e42f44aae6` |
| Tracked-source archive SHA-256, 495364 bytes | `3126c5ac6319df6f49db624c1c36d61e864b002961134aa2b7ecc29cf1d5dfa4` |

The coordinator prepared a clean `--no-ff --no-commit` merge in an isolated
worktree. The review snapshot has no `.git`; its archive hash was independently
verified. The coordinator's Git index inventory records no Tags gitlinks.
Adding this note changes the eventual tree; the receipt above identifies the
reviewed composition before documentation.

## Preservation checks

The following combined files match the exact GitHub blobs from the Express
parent. Git blob comparison normalizes checkout CRLF to LF for text only.

| Path under `src/` | Git blob |
| --- | --- |
| `main/java/org/enthusia/tags/advancements/ExpressAdvancementBridge.java` | `f0c40d21dedfe4bc6089f138e27d2fcc35a543cc` |
| `main/java/org/enthusia/tags/advancements/ExpressStatsReader.java` | `e40c5b04a0297a2b041ecc7236b34fe7e15531f9` |
| `test/java/org/enthusia/tags/advancements/ExpressBridgeTest.java` | `28eebd6cec0fc0c10e54ee0340a7df6870c6db97` |
| `test/java/org/enthusia/tags/advancements/ExpressStatsReaderTest.java` | `1159777980662d8b7a6fed5f1727f2afd2073c77` |

Eleven non-overlapping Pumpkin Patcher paths also match current-main GitHub
blobs: `HolidayTagCatalog.java`, `HolidayTagCatalogTest.java`, the three changed
resource YAML files, the glyph YAML, seed PNG and SVG, holiday pack README,
static-packet validator and `docs/evidence/pumpkin-patcher.md`. The two shared
documents retain both additions: Pumpkin Patcher's REQ-1122 / INFRA-1122 and
Express's REQ-933 through REQ-937 / INFRA-1121. No production or test conflict
required a replacement implementation.

`tools/ci/companions.json` retains renderer repository
`BadgersMC/EnthusiaAdvancements`, commit
`01fac426152737c76cd4d2e570e07a33dde99798`, and version
`1.0.0-pilot.6-watcher-brackets-test.6`. The POM declares that same version and
retains Tags `2.2.3-production-collections-SNAPSHOT`. Existing holiday tags,
reward ownership, native advancement layout and claim authority are preserved.
No network checkout or pin was changed; network pin review and the combined
network build remain separate release gates.

## Verification requirements and result locations

This is an INFRA reconciliation of already reviewed behavior under the existing
SPEAR guidance. It adds no imports, API calls, schema changes or tests. New
behavioral prove/engine steps do not apply, and no historical red/green result
is claimed for this merge. The coordinator owns global task/state updates and
records the actual result of each applicable gate.

| Gate | Required command or evidence location |
| --- | --- |
| EARS and project tooling | `node tools/spear/ears.mjs docs/requirements.md`; `node --test tools/spear/*.test.mjs` |
| Companion validator regressions | `python -m unittest discover -s tools/ci -p 'test_*.py'` |
| Pumpkin Patcher static packet | `python resourcepack/holiday-tags/verify_pumpkin_patcher.py` |
| Immutable dependency bootstrap | Existing `.github/workflows/pr-verify.yml` steps run `tools/bootstrap_loreitems_release.sh` and `tools/ci/bootstrap_companions.sh` with checksum and exact renderer checks retained |
| Full combined regression and package verification | Hosted `mvn -B -ntp clean verify`; retain Surefire counts, failures, errors and skips in the exact-head test-report artifact |
| Final shaded SQLite contract | Hosted `java --class-path target/EnthusiaTags.jar tools/ci/org/enthusia/tools/SQLiteArtifactProbe.java` |
| PR readiness | [PR #12 checks](https://github.com/FainNeito/EnthusiaTags/pull/12/checks), including verification, Codacy gate and applicable artifact workflow results for the final pushed head |
| Artifact provenance | Record workflow head SHA, run URL, artifact identity and downloaded JAR SHA-256 in the coordinator's delivery evidence |

The source receipt establishes preservation only. Build/check results must be
read from the final PR head and its retained workflow artifacts; results from
either parent do not verify this combination. This worker ran no Maven build
and made no server or client change. Actual provider/client acceptance, upstream
canonical merge status, network integration and production activation require
their own evidence and are not established by this document or by green CI.
