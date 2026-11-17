package de.jexcellence.dependency.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RepositoryCredentialsTest {

    private static final String VARIABLE = "JEDEPENDENCY_AUTH_REPO_EXAMPLE_ORG";

    @Test
    @DisplayName("derives the variable name from the host")
    void variableName() {
        assertEquals(VARIABLE, RepositoryCredentials.variableFor("repo.example.org"));
        assertEquals("JEDEPENDENCY_AUTH_MY_REPO_EXAMPLE_ORG", RepositoryCredentials.variableFor("my-repo.example.org"));
    }

    @Test
    @DisplayName("sends user:password as HTTP Basic and keeps a bearer token")
    void headerValues() {
        final RepositoryCredentials basic = new RepositoryCredentials(Map.of(VARIABLE, "deploy:s3cret")::get);
        final String expected = "Basic " + Base64.getEncoder().encodeToString("deploy:s3cret".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, basic.authorizationFor(URI.create("https://repo.example.org/maven/a.jar")));

        final RepositoryCredentials bearer = new RepositoryCredentials(Map.of(VARIABLE, "Bearer abc")::get);
        assertEquals("Bearer abc", bearer.authorizationFor(URI.create("https://repo.example.org/a.jar")));
    }

    @Test
    @DisplayName("never sends credentials over http, to other hosts or when the value is malformed")
    void refusals() {
        final RepositoryCredentials credentials = new RepositoryCredentials(Map.of(VARIABLE, "deploy:s3cret")::get);
        assertNull(credentials.authorizationFor(URI.create("http://repo.example.org/a.jar")));
        assertNull(credentials.authorizationFor(URI.create("https://cdn.example.org/a.jar")));

        final RepositoryCredentials malformed = new RepositoryCredentials(Map.of(VARIABLE, "no-colon")::get);
        assertNull(malformed.authorizationFor(URI.create("https://repo.example.org/a.jar")));
    }
}
