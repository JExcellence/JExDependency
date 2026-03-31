/**
 * Shared enumerations that coordinate statistic definitions across modules.
 *
 * <p><strong>Runtime behaviour mapping</strong>
 * <p>{@link EStatisticType} centralises the keys, default values, and categories referenced by RDQ
 * requirements and RCore onboarding flows. Player bootstrap listeners populate core defaults from the enum,
 * while gameplay modules query the same constants to evaluate quest prerequisites and perk unlocks.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/type/EStatisticType.java†L35-L330】【F:RCore/rcore-free/src/main/java/com/raindropcentral/core/listener/PlayerPreLogin.java†L139-L231】【F:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/requirement/RStatisticRequirement.java†L7-L266】</p>
 *
 * <p><strong>Maintaining compatibility</strong>
 * <p>When adding or renaming statistics, update the default maps in the enum and communicate the change to
 * RDQ and RCore so their migrations can seed the new keys. Because the enum also exposes convenience
 * lookups like {@link EStatisticType#getDefaultValuesForCategory(EStatisticType.StatisticCategory)},
 * keep category membership stable to avoid breaking dashboards or analytics jobs.</p>
 */
package com.raindropcentral.rplatform.type;
