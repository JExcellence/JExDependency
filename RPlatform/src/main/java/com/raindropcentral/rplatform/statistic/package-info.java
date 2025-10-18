/**
 * Defines the immutable catalogue of platform statistics exposed to gameplay modules.
 *
 * <p>{@link com.raindropcentral.rplatform.statistic.StatisticType} centralises statistic identifiers, their expected data
 * types, default values, and human-readable descriptions so both free and premium services can bootstrap consistent
 * player records after {@link com.raindropcentral.rplatform.RPlatform#initialize()} completes. Categories help downstream
 * dashboards and storage layers group related metrics without duplicating constants.</p>
 *
 * <p>When introducing new statistics, append additional enum constants with unique keys rather than renaming or removing
 * existing entries; this preserves compatibility with historical data and third-party dashboards. Update the associated
 * {@link com.raindropcentral.rplatform.statistic.StatisticType.Category} or
 * {@link com.raindropcentral.rplatform.statistic.StatisticType.DataType} only when absolutely necessary, and provide sane
 * defaults so freshly provisioned player rows remain back-fill friendly.</p>
 */
package com.raindropcentral.rplatform.statistic;
