# Holiday rewards — SPEAR

Base: wsg138/EnthusiaTags main 28048ca. Scope: source PR, local verification and interactive preview; no server activation.

Spec: REQ-917..919. Existing root menu stops after seven categories, so merely appending Holidays would hide it. Canonical configuration lacks holiday tag definitions. SMP Test's Pumpkin Hunter uses an orange gradient without bold; custom Pumpkin King crown styling must be retained.

Prove: migration and category tests cover preserving custom fields, bold styling, parent validation, configured catalog entries, and root/child pagination. Catalog entries display ownership only; EnthusiaHolidays owns event awards.

Engine/arch/refine: after integrating the existing PR #29 holiday catalog and crown assets, 258 Maven tests and shaded artifact checks pass; EARS and 12 Node tooling tests pass. HolidayMenuTest checks visible root categories, parent navigation, catalog ownership with no claims, and bounded pagination using Bukkit/item mocks. Preserve reward IDs, claim semantics, permissions and existing tag ownership. Native client acceptance remains pending.

Preview: https://enthusia-holiday-rewards-preview.awareyak.chatgpt.site (private, requires owner ChatGPT login). Local browser QA at 390px verified no horizontal document overflow, Halloween/Christmas navigation and earned/locked states. The IAB's published-site login encountered security verification; authenticated mobile access is not independently confirmed. Local Sites archive packaging is unavailable because its Bash dependency is absent; exact pushed source was published using the supported remote-build fallback.
