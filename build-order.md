# Build Order for RaindropCentral Projects

Due to circular compile-time dependencies between RPlatform and JExEconomy, follow this build order:

## Initial Setup (First Time Only)

1. Build RPlatform (it will compile with JExEconomy classes available via `compileOnly`):
   ```bash
   ./gradlew :RPlatform:build :RPlatform:publishToMavenLocal
   ```

2. Build JExEconomy (now that RPlatform is available):
   ```bash
   ./gradlew :JExEconomy:jexeconomy-common:build
   ```

## Regular Development

After initial setup, you can build normally:
```bash
./gradlew build
```

## Why This Works

- RPlatform uses `compileOnly(project(":JExEconomy:jexeconomy-common"))` 
  - This means JExEconomy classes are available at compile time
  - But they're NOT packaged into RPlatform JAR
  - RPlatform uses Bukkit's service system to find JExEconomy at runtime

- JExEconomy depends on RPlatform normally
  - It needs RPlatform classes both at compile and runtime

## Troubleshooting

If you get "cannot find symbol" errors for `CentralLogger.getLoggerByName()`:
- Make sure you're using the latest RPlatform version
- Run `./gradlew :RPlatform:clean :RPlatform:build :RPlatform:publishToMavenLocal`
