/**
 * Rank tree configuration bridging quest progress to promotion rules.
 * <p>
 * The asynchronous enable stage (stage&nbsp;1) prepares the executor used when resolving tree
 * metadata. Subsequent component and view wiring (stage&nbsp;2) run inside the
 * {@link com.raindropcentral.rdq.RDQ#runSync(Runnable) runSync} block, ensuring any tree previews
 * rendered in GUIs stay aligned with the immutable configuration model.
 * </p>
 * <p>
 * Stage&nbsp;3 wires {@link com.raindropcentral.rdq.database.repository.RRankTreeRepository} and
 * {@link com.raindropcentral.rdq.database.repository.RRequirementRepository}, both of which
 * consume the YAML assets referenced in {@code rdq-common/src/main/resources/rank/README.md}.
 * Premium- and free-edition managers share these repositories, so contributors should coordinate
 * updates with the lifecycle notes recorded on {@code RDQFreeManager} and {@code RDQPremiumManager}.
 * </p>
 */
package com.raindropcentral.rdq.config.ranks.ranktree;
