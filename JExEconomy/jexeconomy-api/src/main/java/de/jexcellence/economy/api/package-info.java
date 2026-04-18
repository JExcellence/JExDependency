/**
 * Public API for the JExEconomy multi-currency economy system.
 *
 * <p>Third-party plugins should depend on this module ({@code jexeconomy-api})
 * as {@code compileOnly} and retrieve the {@link de.jexcellence.economy.api.EconomyProvider}
 * via {@link de.jexcellence.economy.api.JExEconomyAPI#get()}.
 *
 * <h2>Maven dependency:</h2>
 * <pre>{@code
 * <dependency>
 *   <groupId>de.jexcellence.economy</groupId>
 *   <artifactId>jexeconomy-api</artifactId>
 *   <version>3.0.0</version>
 *   <scope>provided</scope>
 * </dependency>
 * }</pre>
 *
 * <h2>Gradle dependency:</h2>
 * <pre>{@code
 * compileOnly("de.jexcellence.economy:jexeconomy-api:3.0.0")
 * }</pre>
 *
 * @author JExcellence
 * @since 3.0.0
 */
package de.jexcellence.economy.api;
