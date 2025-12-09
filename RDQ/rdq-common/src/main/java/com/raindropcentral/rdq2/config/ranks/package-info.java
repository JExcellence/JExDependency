/**
 * Rank configuration sections.
 * <p>
 * Rank, rank-tree, and system settings sections mirror the YAML documents located in
 * {@code rdq-common/src/main/resources/rank/}. {@link com.raindropcentral.rdq2.config.ranks.rank.RankSection}
 * exposes per-rank requirements and rewards, while {@link com.raindropcentral.rdq2.config.ranks.system.RankSystemSection}
 * and related classes coordinate progression logic. Parsed data feeds {@link com.raindropcentral.rdq2.utility.rank.RankRequirementContext}
 * and persistence models in {@link com.raindropcentral.rdq2.database.entity.rank}.
 * </p>
 * <p>
 * Update both the configuration sections and the YAML resources when introducing new keys, then run
 * {@code ./gradlew :RDQ:rdq-common:check} to validate mapper compatibility.
 * </p>
 */
package com.raindropcentral.rdq2.config.ranks;
