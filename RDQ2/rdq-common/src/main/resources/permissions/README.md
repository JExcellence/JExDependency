# Permission Map

`permissions.yml` is read during stage 2 while
[`RDQ#initializeComponents()`](../../java/com/raindropcentral/rdq/RDQ.java) registers commands and
listeners inside the [`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) boundary. The mapping is
then cached so both editions present consistent permission trees even when executor fallback occurs on
stage 1.

When modifying permissions, reference the command README and the lifecycle Javadocs for `RDQFreeImpl`
and `RDQPremiumImpl` so the free and premium command surfaces stay synchronized.
