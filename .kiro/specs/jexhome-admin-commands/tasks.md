# Implementation Plan

- [x] 1. Create admin command infrastructure




  - [x] 1.1 Create EPAdminHomePermission enum with ADMIN_SETHOME and ADMIN_DELHOME permissions


    - Define permission nodes following existing pattern (EPSetHomePermission)


    - _Requirements: 4.1, 4.2_




  - [ ] 1.2 Create PAdminHomeSection command configuration class
    - Extend ACommandSection with "admin" command name

    - _Requirements: 1.1, 2.1_
  - [ ] 1.3 Create padminhome.yml command configuration file
    - Define command name, aliases, usage, and permission nodes

    - _Requirements: 1.4, 2.4, 4.1, 4.2_

- [x] 2. Implement PAdminHome command handler

  - [ ] 2.1 Create PAdminHome class extending PlayerCommand
    - Implement onPlayerInvocation for subcommand routing (sethome/delhome)
    - Implement player UUID resolution for online and offline players



    - _Requirements: 1.1, 1.5, 2.1, 2.5_



  - [x] 2.2 Implement handleSetHome method




    - Resolve target player UUID, create home at admin's location

    - Handle errors (player not found, internal errors)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - [ ] 2.3 Implement handleDelHome method
    - Resolve target player UUID, delete specified home
    - Handle errors (player not found, home not found)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [ ] 2.4 Implement tab completion for player names and home names
    - Suggest online players for first argument
    - Suggest target player's homes for second argument in delhome
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 3. Add translation keys for admin commands
  - [ ] 3.1 Add English (en_US) translation keys for admin commands
    - Add success, error, and usage messages
    - _Requirements: 1.3, 2.2, 4.3_
  - [ ] 3.2 Add German (de_DE) translation keys for admin commands
    - Add success, error, and usage messages
    - _Requirements: 1.3, 2.2, 4.3_

- [ ] 4. Register admin command in JExHome plugin
  - [ ] 4.1 Update JExHome to register PAdminHome command
    - Add command registration in initializeComponents or similar
    - _Requirements: 1.1, 2.1_
