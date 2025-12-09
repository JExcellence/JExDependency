/**
 * Player-facing views that bridge RDQ services with the unified UI framework.
 * <p>
 * Views extend {@link com.raindropcentral.rplatform.view.BaseView} and rely on
 * edition-aware services supplied by managers. Navigation is centralized so the same frame keys and
 * slot layouts work across free and premium editions, letting managers swap service implementations
 * without altering UI flows.
 * </p>
 */
package com.raindropcentral.rdq2.view;
