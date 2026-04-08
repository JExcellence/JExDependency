# Implementation Plan

- [x] 1. Add Floodgate API dependency and detection





  - [x] 1.1 Add Floodgate API dependency to JExHome pom.xml




    - Add Geyser repository to repositories section
    - Add Floodgate API dependency with provided scope
    - _Requirements: 1.1, 1.2, 4.3_

  - [x] 1.2 Create PlayerTypeDetector utility class

    - Create `de.jexcellence.home.utility.PlayerTypeDetector`
    - Implement `initialize()` method with ClassNotFoundException handling
    - Implement `isBedrockPlayer(Player)` method using Floodgate API
    - Implement `isFloodgateAvailable()` method
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 1.3 Initialize PlayerTypeDetector on plugin enable

    - Call `PlayerTypeDetector.initialize()` in JExHome onEnable
    - Log Floodgate availability status
    - _Requirements: 4.1, 4.2_

- [x] 2. Create Bedrock form classes





  - [x] 2.1 Create BedrockHomeOverviewForm


    - Create `de.jexcellence.home.view.bedrock.BedrockHomeOverviewForm`
    - Implement `show(Player, JExHome)` method
    - Build SimpleForm with home list buttons
    - Add "Create New Home" button at bottom
    - Handle button clicks for teleport and create actions
    - _Requirements: 2.1, 2.4_

  - [x] 2.2 Create BedrockSetHomeForm

    - Create `de.jexcellence.home.view.bedrock.BedrockSetHomeForm`
    - Implement `show(Player, JExHome)` method
    - Build CustomForm with text input for home name
    - Validate home name using existing pattern
    - Call HomeFactory to create home on valid input
    - _Requirements: 2.2_

  - [x] 2.3 Create BedrockDeleteHomeForm

    - Create `de.jexcellence.home.view.bedrock.BedrockDeleteHomeForm`
    - Implement `show(Player, JExHome, Home)` method
    - Build ModalForm with confirm/cancel buttons
    - Display home details in form content
    - Handle deletion on confirm
    - _Requirements: 2.3_

- [x] 3. Create ViewRouter and integrate with commands





  - [x] 3.1 Create ViewRouter class


    - Create `de.jexcellence.home.view.ViewRouter`
    - Inject JExHome and ViewFrame dependencies
    - Implement `openHomeOverview(Player)` with Bedrock detection
    - Implement `openSetHome(Player)` with Bedrock detection
    - Implement `openDeleteConfirmation(Player, Home)` with Bedrock detection
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2_

  - [x] 3.2 Update PHome command to use ViewRouter

    - Replace direct ViewFrame.open calls with ViewRouter methods
    - _Requirements: 3.1, 3.2_

  - [x] 3.3 Update PSetHome command to use ViewRouter

    - Replace direct ViewFrame.open calls with ViewRouter methods
    - _Requirements: 3.1, 3.2_
  - [x] 3.4 Update PDelHome command to use ViewRouter


    - Replace direct ViewFrame.open calls with ViewRouter methods
    - _Requirements: 3.1, 3.2_

- [x] 4. Add configuration options






  - [x] 4.1 Add Bedrock configuration section to HomeSystemConfig

    - Add `bedrockEnabled` boolean field (default: true)
    - Add `forceChestGui` boolean field (default: false)
    - Add getters for both fields
    - _Requirements: 5.1, 5.2_

  - [x] 4.2 Update home-system.yml with Bedrock section

    - Add `bedrock.enabled` option
    - Add `bedrock.force-chest-gui` option
    - Add comments explaining each option
    - _Requirements: 5.1, 5.2_

  - [x] 4.3 Update ViewRouter to respect configuration

    - Check `bedrockEnabled` before using Bedrock forms
    - Check `forceChestGui` to override Bedrock detection
    - _Requirements: 5.1, 5.2, 5.3_

- [x] 5. Testing and documentation





  - [x] 5.1 Write unit tests for PlayerTypeDetector


    - Test detection with mocked Floodgate API
    - Test fallback when Floodgate unavailable
    - _Requirements: 1.1, 1.2, 1.3_


  - [ ] 5.2 Add Bedrock support documentation
    - Document configuration options
    - Document Floodgate/Geyser requirements
    - _Requirements: 5.1, 5.2_
