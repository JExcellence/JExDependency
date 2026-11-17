# Security policy

JExDependency downloads jars and loads them into a running Minecraft server. A flaw in how it chooses, downloads,
verifies or loads those jars can let someone run code on servers that use it, so security reports are handled first.

## Supported versions

| Version | Security fixes |
|---|---|
| Latest release | Yes |
| Older releases | No, update to the latest release |

## Reporting a vulnerability

Please do not open a public issue. Use one of these private channels:

- GitHub: the **Report a vulnerability** button under the repository's **Security** tab.
- Email: [contact@jexcellence.de](mailto:contact@jexcellence.de) with "JExDependency security" in the subject.

Include the affected version, the server software and Java version, and steps or a proof of concept. You will get a
first answer within 7 days. Once a fix is released, the advisory is published and you are credited unless you ask
not to be.

## In scope

- Downloading from a repository or URL other than the configured ones.
- Accepting a jar whose published checksum does not match.
- Path traversal or overwriting files outside the cache and plugin folders.
- A malicious repository response or POM causing code execution before the jar is loaded.

## Out of scope

- Code inside a library you chose to load. JExDependency loads what you declare; the library runs with your
  plugin's rights.
- Repositories that publish no checksum. Such jars are accepted by design; see "Network and security" in the README.
