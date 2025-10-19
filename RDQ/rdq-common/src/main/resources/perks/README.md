# Perk Catalog

Perk YAML definitions are parsed in stage 3 alongside
[`RDQ#initializeRepositories()`](../../java/com/raindropcentral/rdq/RDQ.java), which hydrates the
`RPerkRepository`. The work executes on the stage-1 executor (virtual threads with a fixed-pool
fallback) and surfaces immutable models consumed by the perk sections documented under
`com.raindropcentral.rdq.config.perk.sections`.

Both editions access the hydrated data after stage 2 completes, when the perk managers are wired
inside the [`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) block. Align schema changes with
the RDQ manager Javadocs to keep free and premium functionality synchronized.
