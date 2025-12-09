# RDQ Runtime Dependencies

The descriptors in this directory (and their `paper/` and `spigot/` variants) are processed during the
pre-enable `onLoad` hook when `RDQFree` and `RDQPremium` invoke `JEDependency.initializeWithRemapping`.
This work occurs before stage 1 of the RDQ enable pipeline so the virtual-thread executor is ready by
the time repositories and views request shaded libraries.

## Structure

- `dependencies.yml` - Default dependencies for all server types
- `paper/dependencies.yml` - Paper-specific dependencies (includes Paper-specific inventory framework)
- `spigot/dependencies.yml` - Spigot-specific dependencies

## How It Works

1. During `onLoad()`, `JEDependency.initializeWithRemapping()` is called
2. JEDependency detects the server type (Paper, Spigot, etc.)
3. The appropriate dependencies.yml is loaded
4. Dependencies are downloaded from Maven Central if not cached
5. Classes are loaded into the plugin's classloader with proper remapping

## Adding Dependencies

To add a new dependency, add it to all three files in the format:
```yaml
- groupId:artifactId:version
```

Example:
```yaml
- com.example:my-library:1.0.0
```
