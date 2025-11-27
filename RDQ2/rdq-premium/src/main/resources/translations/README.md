# Translation Bundles

Translation files load in stage 3 when
[`RDQ#initializeRepositories()`](../../java/com/raindropcentral/rdq/RDQ.java) connects the shared
i18n repository. Translation hydration uses the stage-1 executor (virtual threads with fixed-pool
fallback) to prepare message catalogs off the main thread before views render them inside the
[`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) boundary.

Both editions consume the same bundles through the shared view frame. Coordinate updates with the
RDQ manager and bounty service Javadocs to avoid divergences between free and premium UX.
