# Holiday rewards — SPEAR

Base: wsg138/EnthusiaTags main 28048ca. Scope: source PR, local verification and interactive preview; no server activation.

Spec: REQ-917..919. Existing root menu stops after seven categories, so merely appending Holidays would hide it. Canonical configuration lacks holiday tag definitions. SMP Test's Pumpkin Hunter uses an orange gradient without bold; custom Pumpkin King crown styling must be retained.

Prove: migration and category tests cover preserving custom fields, bold styling, parent validation, configured catalog entries, and root/child pagination. Catalog entries display ownership only; EnthusiaHolidays owns event awards.

Engine/arch/refine: 253 Maven tests and shaded artifact checks pass; EARS and 12 Node tooling tests pass. Preserve reward IDs, claim semantics, permissions and existing tag ownership. Native client acceptance remains pending.
