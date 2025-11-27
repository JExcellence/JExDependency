# Logging Configuration

`logging.yml` is applied during stage 1 when the shared `RPlatform` bootstraps logging facilities for
`RDQ` before entering the [`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) portion of enable.
The configuration remains immutable through stages 2 and 3 so console output stays consistent while
commands, views, and repositories come online.

Updates affect both editions immediately because `RDQFreeImpl` and `RDQPremiumImpl` rely on the same
`CentralLogger` initialisation path before wiring managers in stage 2.
