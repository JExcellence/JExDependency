# RCore - Raindrop Central Core Plugin

A unified Minecraft plugin providing core functionality for the Raindrop plugin ecosystem.

## Features

- 🎮 **Player Management** - Persistent player data with async operations
- 🖥️ **Server Registration** - Automatic server tracking and identification
- 📊 **Statistics System** - Flexible player statistics with custom templates
- 🗄️ **Database Support** - H2 (file/memory) and MySQL with Hibernate/JPA
- ⚡ **Async Operations** - Virtual thread executor for non-blocking persistence
- 🔌 **Plugin Integration** - Automatic detection of supported plugins
- 📈 **Metrics** - bStats integration for usage analytics
- 🎨 **Adventure API** - Modern text components and formatting
- 🏗️ **Repository Pattern** - Clean separation of concerns
- 🔧 **Dependency Management** - Automatic dependency loading and remapping

## Installation

### Requirements
- Java 21 or higher
- Paper/Spigot 1.16+ (API version 1.19+ recommended)
- 50MB+ free disk space (for dependencies and database)

### Steps

1. **Download** the latest `RCore-2.0.0.jar` from releases
2. **Place** in your server's `plugins/` directory
3. **Start** your server
4. **Configure** (optional) - edit `plugins/RCore/database/hibernate.properties`

## Configuration

### Database Configuration

Edit `plugins/RCore/database/hibernate.properties`:

```properties
# H2 File-based (default)
jakarta.persistence.jdbc.url=jdbc:h2:file:./plugins/RCore/database/testdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
database.type=h2

# MySQL
jakarta.persistence.jdbc.url=jdbc:mysql://localhost:3306/rcore
jakarta.persistence.jdbc.user=your_username
jakarta.persistence.jdbc.password=your_password
database.type=mysql
database.host=localhost
database.port=3306
database.name=rcore
```

### Supported Databases
- **H2** - File-based or in-memory (default, no setup required)
- **MySQL** - Production-ready relational database

## Usage

### For Server Owners

RCore runs automatically in the background. No commands or permissions needed.

**Verify Installation:**
```
/version RCore
```

**Check Logs:**
```
[RCore] RCore loaded successfully
[RCore] Registered RCoreService provider with priority NORMAL
[RCore] Detected X/15 supported plugins
[RCore] Server registered: YourServerName (uuid)
[RCore] RCore enabled successfully
```

### For Developers

#### Depend on RCore

**plugin.yml:**
```yaml
depend: [RCore]
# or
softdepend: [RCore]
```

**build.gradle.kts:**
```kotlin
dependencies {
    compileOnly(project(":RCore"))
    // or from Maven
    compileOnly("com.raindropcentral.core:rcore:2.0.0")
}
```

#### Access RCore Service

```java
import com.raindropcentral.core.service.RCoreService;
import org.bukkit.Bukkit;

public class MyPlugin extends JavaPlugin {
    
    private RCoreService rCoreService;
    
    @Override
    public void onEnable() {
        // Get RCore service
        this.rCoreService = Bukkit.getServicesManager()
                .getRegistration(RCoreService.class)
                .getProvider();
        
        // Use the service
        UUID playerUuid = ...;
        rCoreService.findByUuidAsync(playerUuid)
                .thenAccept(optionalPlayer -> {
                    optionalPlayer.ifPresent(player -> {
                        getLogger().info("Found player: " + player.getPlayerName());
                    });
                });
    }
}
```

#### Available Operations

```java
// Player operations
CompletableFuture<Optional<RPlayer>> findByUuidAsync(UUID uniqueId);
CompletableFuture<Optional<RPlayer>> findByNameAsync(String playerName);
CompletableFuture<RPlayer> createAsync(RPlayer player);
CompletableFuture<RPlayer> updateAsync(RPlayer player);

// Access repositories directly
RPlayerRepository playerRepo = rCoreService.getRPlayerRepository();
RPlayerStatisticRepository statsRepo = rCoreService.getRPlayerStatisticRepository();
RStatisticRepository statTemplateRepo = rCoreService.getRStatisticRepository();
RServerRepository serverRepo = rCoreService.getRServerRepository();
```

## Architecture

### Project Structure
```
RCore/
├── rcore-common/           # Shared libraries and services
│   ├── api/                # Public API interfaces
│   ├── database/           # JPA entities and repositories
│   ├── service/            # Business logic services
│   └── ...
└── src/                    # Main plugin
    ├── main/java/
    │   └── com/raindropcentral/core/
    │       ├── RCore.java          # Plugin entry point
    │       └── RCoreImpl.java      # Implementation delegate
    └── main/resources/
        ├── plugin.yml              # Bukkit plugin descriptor
        ├── paper-plugin.yml        # Paper plugin descriptor
        ├── database/               # Database configuration
        └── dependency/             # Dependency definitions
```

### Key Components

- **RCore** - Bukkit plugin entry point, handles lifecycle
- **RCoreImpl** - Implementation delegate, wires services
- **RPlatform** - Platform abstraction for DI, metrics, persistence
- **RCoreService** - Public API exposed via Bukkit services
- **Repositories** - Data access layer with async operations
- **Entities** - JPA entities (RPlayer, RServer, RStatistic, etc.)

### Lifecycle

1. **onLoad()** - Initialize logging, platform, register service
2. **onEnable()** - Async startup: platform → repositories → plugins → server registration → metrics
3. **onDisable()** - Unregister service, shutdown executor

## Supported Plugins

RCore automatically detects and integrates with:

- Aura
- ChestSort
- CMI
- DiscordSRV
- EcoJobs
- Essentials (Chat, Discord, Spawn)
- HuskTowns
- Jobs
- JExEconomy
- LuckPerms
- mcMMO
- MysticMobs
- ProtocolLib
- RDR
- Towny (+ TownyChat)

## Building from Source

### Requirements
- Java 21 JDK
- Gradle 8.0+

### Build Steps

```bash
# Clone repository
git clone <repository-url>
cd RaindropPlugins

# Build RCore
./gradlew :RCore:shadowJar

# Output: RCore/build/libs/RCore-2.0.0.jar
```

### Development Build

```bash
# Clean build
./gradlew :RCore:clean :RCore:shadowJar

# Publish to local Maven
./gradlew :RCore:publishLocal

# Run tests
./gradlew :RCore:test
```

## Migration from Free/Premium

If upgrading from the old RCore-Free or RCore-Premium:

1. **No configuration changes needed**
2. **Remove old plugin** (`RCore-free-2.0.0.jar` or `RCore-premium-2.0.0.jar`)
3. **Install new plugin** (`RCore-2.0.0.jar`)
4. **Restart server**

See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for detailed instructions.

## Troubleshooting

### Plugin Won't Load

**Check Java version:**
```bash
java -version
# Should be 21 or higher
```

**Check logs:**
```
[RCore] Failed to load RCore
```

**Common causes:**
- Java version < 21
- Conflicting plugins
- Corrupted JAR file
- Insufficient permissions

### Database Issues

**H2 locked:**
```
Database may be already in use
```
**Solution:** Stop all servers using the same database file

**MySQL connection failed:**
```
Communications link failure
```
**Solution:** Check MySQL is running, credentials are correct, firewall allows connection

### Performance Issues

**Slow startup:**
- Normal on first run (dependency download)
- Check disk I/O and network speed

**High memory usage:**
- Hibernate caches entities
- Adjust `hibernate.connection.pool_size` in config

## Support

- 📖 **Documentation**: This README and [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)
- 🐛 **Bug Reports**: Create an issue on GitHub
- 💬 **Discord**: Join our community server
- 🌐 **Website**: [RaindropCentral.com](https://raindropcentral.com)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

- **Product of**: Antimatter Zone LLC
- **Technology Partner**: JExcellence
- **Authors**: ItsRainingHP, JExcellence

## Changelog

### Version 2.0.0 (Current)
- ✨ Consolidated free/premium into single unified plugin
- 🏗️ Simplified project structure
- 📦 Single JAR distribution
- 🔧 Improved build configuration
- 📚 Enhanced documentation

### Version 1.x
- Initial free/premium split releases
- Core functionality implementation
- Database integration
- Plugin ecosystem support
