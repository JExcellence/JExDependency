package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.repository.RepositoryUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Reads {@code dependencies.yml}. Only the small YAML subset the file uses is supported (top-level keys, string
 * lists and booleans), so no YAML library is needed while a plugin bootstraps.
 */
final class DescriptorParser {

    private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final String SHA256_PREFIX = "sha256=";

    private final Consumer<String> warnings;
    private final List<DependencyEntry> entries = new ArrayList<>();
    private final List<String> repositories = new ArrayList<>();
    private final List<String> relocationExcludes = new ArrayList<>();
    private boolean communityRepositories;
    private @Nullable String section;

    private DescriptorParser(@NotNull final Consumer<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * Parses a descriptor. Invalid lines are skipped and reported through {@code warnings}.
     *
     * @param input    UTF-8 YAML content
     * @param warnings receives one message per skipped line
     * @return the parsed descriptor
     * @throws IOException if the stream cannot be read
     */
    static @NotNull DependencyDescriptor parse(@NotNull final InputStream input,
                                               @NotNull final Consumer<String> warnings) throws IOException {
        final DescriptorParser parser = new DescriptorParser(warnings);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                parser.line(line, number);
            }
        }
        return new DependencyDescriptor(parser.entries, parser.repositories, parser.communityRepositories,
                parser.relocationExcludes);
    }

    private void line(@NotNull final String raw, final int number) {
        final String content = stripComment(raw);
        final String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        if (trimmed.startsWith("-")) {
            listItem(unquote(trimmed.substring(1).trim()), number);
            return;
        }

        // Indented keys are read like top-level keys, as earlier versions did.
        final int colon = trimmed.indexOf(':');
        if (colon <= 0) {
            warnings.accept("dependencies.yml line " + number + ": expected 'key:' but got: " + trimmed);
            section = null;
            return;
        }

        final String key = trimmed.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        final String value = unquote(trimmed.substring(colon + 1).trim());
        if (value.isEmpty()) {
            section = key;
            return;
        }

        section = null;
        if ("community-repositories".equals(key)) {
            communityRepositories = Boolean.parseBoolean(value);
        }
    }

    private void listItem(@NotNull final String item, final int number) {
        if (item.isEmpty() || section == null) {
            return;
        }
        switch (section) {
            case "dependencies" -> dependency(item, number);
            case "repositories" -> {
                final String url = RepositoryUrl.parse(item, "dependencies.yml line " + number, warnings);
                if (url != null && !repositories.contains(url)) {
                    repositories.add(url);
                }
            }
            case "relocation-excludes" -> {
                if (PACKAGE_NAME.matcher(item).matches()) {
                    relocationExcludes.add(item);
                } else {
                    warnings.accept("dependencies.yml line " + number + ": not a package name: " + item);
                }
            }
            default -> {
                // Unknown sections belong to newer versions or other tools.
            }
        }
    }

    private void dependency(@NotNull final String item, final int number) {
        final String[] tokens = item.split("\\s+");
        final DependencyCoordinate coordinate = DependencyCoordinate.parse(tokens[0]);
        if (coordinate == null) {
            warnings.accept("dependencies.yml line " + number + ": invalid coordinate: " + tokens[0]);
            return;
        }

        String repository = null;
        String sha256 = null;
        for (int index = 1; index < tokens.length; index++) {
            final String token = tokens[index];
            if ("@".equals(token) && index + 1 < tokens.length) {
                repository = tokens[++index];
            } else if (token.startsWith("@") && token.length() > 1) {
                repository = token.substring(1);
            } else if (token.toLowerCase(Locale.ROOT).startsWith(SHA256_PREFIX)) {
                sha256 = token.substring(SHA256_PREFIX.length());
            } else {
                warnings.accept("dependencies.yml line " + number + ": ignoring unknown option: " + token);
            }
        }

        if (repository != null) {
            repository = RepositoryUrl.parse(repository, "dependencies.yml line " + number, warnings);
            if (repository == null) {
                warnings.accept("dependencies.yml line " + number + ": skipping " + coordinate.toGavString()
                        + " because its repository is invalid");
                return;
            }
        }

        try {
            entries.add(new DependencyEntry(coordinate, repository, sha256));
        } catch (final IllegalArgumentException exception) {
            warnings.accept("dependencies.yml line " + number + ": skipping " + coordinate.toGavString() + ": "
                    + exception.getMessage());
        }
    }

    /**
     * Removes a {@code #} comment that starts the line or follows whitespace outside quotes.
     */
    static @NotNull String stripComment(@NotNull final String line) {
        char quote = 0;
        for (int index = 0; index < line.length(); index++) {
            final char current = line.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if (current == '"' || current == '\'') {
                quote = current;
            } else if (current == '#' && (index == 0 || Character.isWhitespace(line.charAt(index - 1)))) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static @NotNull String unquote(@NotNull final String value) {
        if (value.length() >= 2) {
            final char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}
