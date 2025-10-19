# Bounty Configuration

`bounty.yml` is ingested during stage 3 of enablement when
[`RDQ#initializeRepositories()`](../../java/com/raindropcentral/rdq/RDQ.java) wires the shared
`RBountyRepository`. The repositories execute on the executor prepared in stage 1, preferring virtual
threads and falling back to the fixed thread pool exposed by `RDQ#createExecutor()` to keep IO off
Bukkit's main thread.

Changes propagate automatically to free and premium editions because `RDQFreeImpl` and
`RDQPremiumImpl` both delegate to `RDQ` for repository hydration before the respective bounty
managers or services become observable. Cross-check any updates with the new lifecycle Javadocs for
`FreeBountyManager` and `PremiumBountyService` to maintain behaviour parity.
