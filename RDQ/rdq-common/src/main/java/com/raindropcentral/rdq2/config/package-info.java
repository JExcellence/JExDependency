/**
 * This package previously contained DataFolderProvider, which was removed per request to avoid
 * extra abstraction. Obtain the data folder directly from the platform and pass it to consumers
 * (e.g., PerkConfigurationLoader) as a File/Path.
 */
package com.raindropcentral.rdq2.config;
