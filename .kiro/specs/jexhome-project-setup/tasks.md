# Implementation Plan

- [x] 1. Set up Gradle project structure




  - [ ] 1.1 Create root JExHome build.gradle.kts with buildAll and publishLocal tasks
    - Define group as "de.jexcellence.home" and version as "1.0.0"
    - Add buildAll task depending on free and premium shadowJar


    - Add publishLocal task for all modules


    - _Requirements: 1.1, 1.2_


  - [ ] 1.2 Create JExHome gradle/libs.versions.toml referencing root catalog
    - Add jexhome version entry
    - _Requirements: 1.2_


  - [ ] 1.3 Update root settings.gradle.kts to include JExHome submodules
    - Add include statements for JExHome, jexhome-common, jexhome-free, jexhome-premium
    - _Requirements: 1.6_


  - [ ] 1.4 Create jexhome-common/build.gradle.kts with library conventions
    - Use raindrop.library-conventions and raindrop.dependencies-yml plugins
    - Add all compileOnly dependencies matching RDQ pattern




    - Enable preview features
    - _Requirements: 1.3, 2.1, 2.2, 2.5_
  - [ ] 1.5 Create jexhome-free/build.gradle.kts with shadow conventions
    - Use raindrop.shadow-conventions plugin


    - Implement jexhome-common module
    - Configure shadowJar with relocations
    - _Requirements: 1.4, 2.3_


  - [ ] 1.6 Create jexhome-premium/build.gradle.kts with shadow conventions
    - Use raindrop.shadow-conventions plugin
    - Implement jexhome-common module




    - Configure shadowJar with relocations
    - _Requirements: 1.5, 2.4_

- [x] 2. Implement main plugin classes


  - [ ] 2.1 Create abstract JExHome.java in jexhome-common
    - Follow RDQ pattern with RPlatform, ViewFrame, CommandFactory

    - Add HomeRepository with @InjectRepository
    - Implement onEnable/onDisable lifecycle
    - Add abstract methods for edition-specific behavior




    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  - [ ] 2.2 Create JExHomeFree.java and JExHomeFreeImpl.java in jexhome-free
    - JExHomeFree extends JavaPlugin with JEDependency initialization


    - JExHomeFreeImpl extends AbstractPluginDelegate
    - Implement anonymous JExHome subclass


    - _Requirements: 3.5_
  - [x] 2.3 Create JExHomePremium.java and JExHomePremiumImpl.java in jexhome-premium


    - JExHomePremium extends JavaPlugin with JEDependency initialization
    - JExHomePremiumImpl extends AbstractPluginDelegate




    - Implement anonymous JExHome subclass
    - _Requirements: 3.6_


- [-] 3. Implement database layer

  - [-] 3.1 Create Home.java entity in database/entity


    - Extend BaseEntity from JEHibernate
    - Add fields: homeName, playerUuid, worldName, x, y, z, yaw, pitch
    - Add constructor from Player and setLocation/toLocation methods
    - Use @NotNull/@Nullable annotations
    - _Requirements: 5.1, 5.2, 5.5_
  - [ ] 3.2 Create HomeRepository.java in database/repository
    - Extend CachedRepository<Home, Long, Long>
    - Add findByPlayerUuid and findByPlayerAndName methods
    - _Requirements: 5.3, 5.4_
  - [ ] 3.3 Register HomeRepository in JExHome initialization
    - Use RepositoryManager.register() pattern from RDQ
    - Inject repository into JExHome instance
    - _Requirements: 5.4_

- [ ] 4. Implement command system
  - [ ] 4.1 Create home command classes (PHome, PHomeSection, EPHomePermission)
    - PHome extends PlayerCommand with @Command annotation
    - Opens HomeOverviewView when no args, teleports to named home otherwise
    - Use var and @NotNull/@Nullable annotations
    - _Requirements: 4.1, 4.4, 4.5, 4.6, 4.7_
  - [ ] 4.2 Create sethome command classes (PSetHome, PSetHomeSection, EPSetHomePermission)
    - Check home limit from config before creating
    - Support overwriting existing homes
    - _Requirements: 4.2, 4.4, 4.5, 4.6, 4.7_
  - [ ] 4.3 Create delhome command classes (PDelHome, PDelHomeSection, EPDelHomePermission)
    - Delete home by name
    - Show error if home doesn't exist
    - _Requirements: 4.3, 4.4, 4.5, 4.6, 4.7_
  - [ ] 4.4 Create command configuration YAML files
    - Create commands/home.yml, sethome.yml, delhome.yml
    - Follow RDQ prq.yml pattern with permissions and error messages
    - _Requirements: 8.1_

- [ ] 5. Implement configuration system
  - [ ] 5.1 Create HomeSystemConfig.java section class
    - Implement IConfigSection from bbconfigmapper
    - Add homeLimits map and teleport settings
    - Add getMaxHomesForPlayer method
    - _Requirements: 8.3, 8.4_
  - [ ] 5.2 Create configs/home-system.yml resource file
    - Define default home limits by permission
    - Define teleport delay and cancel settings
    - _Requirements: 8.2_

- [ ] 6. Implement GUI system
  - [x] 6.1 Create HomeOverviewView.java

    - Use inventory-framework patterns from RDQ
    - Display player's homes as clickable items
    - Support pagination for multiple homes
    - Left-click to teleport, right-click for options
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 6.2 Register HomeOverviewView in JExHome ViewFrame initialization
    - Add to ViewFrame.with() during initializeViews()
    - _Requirements: 6.2_

- [x] 7. Implement translation system



  - [x] 7.1 Create translations/en_US.yml with all message keys

    - Include prefix, home.*, sethome.*, delhome.*, teleport.* keys
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 7.2 Create translations/de_DE.yml with German translations

    - Translate all keys from en_US.yml
    - _Requirements: 7.2_

- [x] 8. Create plugin descriptor files



  - [x] 8.1 Create jexhome-free plugin.yml and paper-plugin.yml

    - Use version placeholder substitution
    - Add JEDependency loader for paper-plugin.yml
    - Declare soft dependencies
    - _Requirements: 9.1, 9.2, 9.5_

  - [x] 8.2 Create jexhome-premium plugin.yml and paper-plugin.yml

    - Use version placeholder substitution
    - Add JEDependency loader for paper-plugin.yml
    - Declare soft dependencies
    - _Requirements: 9.3, 9.4, 9.5_

- [x] 9. Create database configuration


  - [x] 9.1 Create database/database.yml resource file


    - Follow RDQ database configuration pattern
    - _Requirements: 2.2_

- [x] 10. Write unit tests



  - [x] 10.1 Create Home entity tests


    - Test location conversion methods
    - _Requirements: 5.1, 5.2_

  - [x] 10.2 Create HomeRepository tests with H2

    - Test findByPlayerUuid and findByPlayerAndName
    - _Requirements: 5.3, 5.4_
