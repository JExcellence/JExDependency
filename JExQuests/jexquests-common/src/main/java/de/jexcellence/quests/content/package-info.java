/**
 * YAML → database content loaders. Each loader walks a data-folder
 * subdirectory, parses every {@code *.yml} file, and upserts the
 * resulting definitions into the corresponding repositories.
 * Nested {@code requirement:} and {@code reward:} sections are
 * serialised straight into the JSON blob columns — the service layer
 * decodes them back into JExCore's sealed-interface domain types.
 */
package de.jexcellence.quests.content;
