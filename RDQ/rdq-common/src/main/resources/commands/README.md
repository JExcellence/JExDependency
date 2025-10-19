# RDQ Command Definitions

These YAML descriptors load during stage 2 of the enable pipeline when
[`RDQ#initializeComponents()`](../../java/com/raindropcentral/rdq/RDQ.java)
invokes the JExCommand registrar inside the [`runSync`](../../java/com/raindropcentral/rdq/RDQ.java)
boundary. The platform executor established in stage 1 (virtual threads with a fixed-pool fallback)
remains available for any asynchronous tab completion the command handlers schedule after the
synchronous registration pass.

Updates propagate to both editions because `RDQFreeImpl` and `RDQPremiumImpl` defer command wiring to
`RDQ`, so keep the notes in the free and premium manager Javadocs in sync when adjusting command
metadata.
