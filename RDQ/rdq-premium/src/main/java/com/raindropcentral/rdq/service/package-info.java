/**
 * Premium service facades.
 *
 * <p>The premium module exposes Bukkit services that wrap repository operations from
 * {@code rdq-common}. Classes here are instantiated once the shared lifecycle reaches the
 * repository wiring stage inside {@link com.raindropcentral.rdq.RDQ}, ensuring that persistence
 * providers registered during platform initialization are ready.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Advertise premium-only service providers (for example the bounty service) via Bukkit's
 *     {@link org.bukkit.plugin.ServicesManager} so external plugins can detect edition capabilities.</li>
 *     <li>Translate shared repository interfaces into public APIs while honoring the asynchronous
 *     execution guarantees established by the common module.</li>
 *     <li>Mirror service registration/unregistration to the free edition so both modules remain in
 *     sync when lifecycle hooks change.</li>
 * </ul>
 *
 * <p>When the shared lifecycle or persistence registry evolves, update these services to bind the
 * new collaborators and keep cross-edition behavior aligned.</p>
 */
package com.raindropcentral.rdq.service;
