# Runtime Dependency Metadata

The descriptors in this directory (and their `paper/` and `spigot/` variants) are processed during the
pre-enable `onLoad` hook when `RDQFree` and `RDQPremium` invoke `JEDependency.initializeWithRemapping`.
This work occurs before stage 1 of the RDQ enable pipeline so the virtual-thread executor is ready by
the time repositories and views request shaded libraries.

Although loading precedes the `runSync` boundary, adjustments here still impact both editions because
the shared RDQ bootstrap validates dependency resolution before entering stage 2. Coordinate changes
with the lifecycle Javadocs to keep documentation aligned.
