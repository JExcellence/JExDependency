# RPlatform Rewards System (`com.raindropcentral.rplatform.reward`)

Last verified from source: March 20, 2026.

This document is the implementation-backed guide for using the RPlatform rewards system across modules, with verified behavior from:

- `RPlatform` core reward classes/factory/service/registry/parser
- `RDQ` reward setup + section adapter + reward usage paths
- `RDR` module source (reward integration review)
- `RDS` module source (reward integration review)

## 1. What This System Is

The rewards system provides:

- Reward type metadata and provider registration (`RewardType`, `RewardRegistry`, `PluginRewardProvider`)
- Runtime reward granting (`RewardService`, `AsyncRewardService`)
- Config-to-object conversion (`RewardFactory`, `RewardSectionAdapter`)
- Fluent programmatic construction (`RewardBuilder`)
- JSON persistence/parsing (`RewardParser`, JPA converters)
- Events, lifecycle hooks, metrics, and validators

Core type registrations come from [`BuiltInRewardProvider.java`](./BuiltInRewardProvider.java):

| Type ID | Implementation Class |
|---|---|
| `ITEM` | `ItemReward` |
| `CURRENCY` | `CurrencyReward` |
| `EXPERIENCE` | `ExperienceReward` |
| `COMMAND` | `CommandReward` |
| `COMPOSITE` | `CompositeReward` |
| `CHOICE` | `ChoiceReward` |
| `PERMISSION` | `PermissionReward` |
| `SOUND` | `SoundReward` |
| `PARTICLE` | `ParticleReward` |
| `TELEPORT` | `TeleportReward` |
| `VANISHING_CHEST` | `VanishingChestReward` |

## 2. Initialization and Type Availability

### 2.1 Core runtime initialization

`RPlatform.initialize()` currently initializes requirements, not rewards. Reward usage is still possible through:

- `RewardFactory` default converters (map-based creation)
- `RewardBuilder` (programmatic creation)
- `RewardParser` for reward classes known to Jackson polymorphic mapping in [`AbstractReward.java`](./AbstractReward.java)

### 2.2 Parser/type-registration behavior to account for

- `RewardParser` relies on `@JsonTypeInfo/@JsonSubTypes` in `AbstractReward`.
- `RewardRegistry.configureObjectMapper(...)` is currently a no-op.
- `AbstractReward` subtype list includes `ITEM`, `CURRENCY`, `EXPERIENCE`, `COMMAND`, `COMPOSITE`, `CHOICE`, `PERMISSION`, `TELEPORT`, `PARTICLE`, `VANISHING_CHEST`.
- `SOUND` is registered as a core type but is not present in `AbstractReward` subtype mapping.

If you add custom reward types and need JSON deserialization, you must wire subtype registration for Jackson and then call:

```java
RewardParser.resetMapper();
```

## 3. Runtime Flow

1. Build reward objects from map config (`RewardFactory.fromMap`), section adapters (`fromSection`), programmatic builders (`RewardBuilder`), or JSON (`RewardParser.parse`).
2. Optionally validate with `RewardValidators.validate(...)`.
3. Grant with `RewardService.grant(player, reward)` or `grantAll(...)`.
4. React to lifecycle/events/metrics:
   - Events: `RewardGrantEvent`, `RewardGrantedEvent`, `RewardFailedEvent`
   - Hooks: `LifecycleRegistry` + `RewardLifecycleHook`
   - Metrics: `RewardMetrics`

Important behavior:

- `ChoiceReward.grant(player)` intentionally returns `false`; use `grantChoices(player, selectedIndices)` for actual grant.

## 4. Core Reward Types + Examples

Examples below use the map/config shape expected by `RewardFactory.fromMap(...)` unless explicitly noted.

### 4.1 `ITEM`

Required:

- `type: ITEM`
- `item` map (`material`, optional `amount`)

```yml
type: ITEM
item:
  material: DIAMOND
  amount: 16
```

### 4.2 `CURRENCY`

Required:

- `type: CURRENCY`

Optional:

- `currencyId` (default `vault`)
- `amount` (default `0.0`)

```yml
type: CURRENCY
currencyId: vault
amount: 2500
```

### 4.3 `EXPERIENCE`

Required:

- `type: EXPERIENCE`

Optional:

- `amount` (default `0`)
- `experienceType` (`POINTS` or `LEVELS`, default `POINTS`)

```yml
type: EXPERIENCE
amount: 30
experienceType: LEVELS
```

### 4.4 `COMMAND`

Required:

- `type: COMMAND`
- `command`

Optional:

- `executeAsPlayer` (default `false`)
- `delayTicks` (default `0`)

Placeholders supported by implementation:

- `{player}`, `{uuid}`, `{world}`, `{x}`, `{y}`, `{z}`

```yml
type: COMMAND
command: "give {player} minecraft:golden_apple 3"
executeAsPlayer: false
delayTicks: 20
```

### 4.5 `COMPOSITE`

Required:

- `type: COMPOSITE`
- `rewards` list

Optional:

- `continueOnError` (default `false`)

```yml
type: COMPOSITE
continueOnError: true
rewards:
  - type: CURRENCY
    currencyId: vault
    amount: 1000
  - type: EXPERIENCE
    amount: 500
    experienceType: POINTS
```

### 4.6 `CHOICE`

Required:

- `type: CHOICE`
- `choices` list

Optional:

- `minimumRequired` (default `1`)
- `maximumRequired`
- `allowMultipleSelections` (default `false`)

```yml
type: CHOICE
minimumRequired: 1
maximumRequired: 1
allowMultipleSelections: false
choices:
  - type: ITEM
    item:
      material: EMERALD
      amount: 32
  - type: CURRENCY
    currencyId: vault
    amount: 5000
```

### 4.7 `PERMISSION`

Required:

- `type: PERMISSION`
- `permissions` (string or list)

Optional:

- `durationSeconds` (temporary grants)
- `temporary` (default `false`; auto-true when `durationSeconds` is set in implementation)

```yml
type: PERMISSION
permissions:
  - raindrop.rank.veteran
  - raindrop.chat.color
durationSeconds: 86400
temporary: true
```

### 4.8 `SOUND`

Core type exists, but default map conversion is not registered in `RewardFactory`.

Use builder or custom converter:

```java
AbstractReward reward = RewardBuilder.sound()
    .sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP)
    .volume(1.0f)
    .pitch(1.0f)
    .build();
```

Representative JSON shape:

```json
{
  "type": "SOUND",
  "sound": "ENTITY_PLAYER_LEVELUP",
  "volume": 1.0,
  "pitch": 1.0
}
```

### 4.9 `PARTICLE`

Core type exists, but default map conversion is not registered in `RewardFactory`.

Builder example:

```java
AbstractReward reward = RewardBuilder.particle()
    .particle(org.bukkit.Particle.HAPPY_VILLAGER)
    .count(20)
    .offset(0.5, 1.0, 0.5)
    .extra(0.1)
    .build();
```

Representative JSON shape:

```json
{
  "type": "PARTICLE",
  "particle": "HAPPY_VILLAGER",
  "count": 20,
  "offsetX": 0.5,
  "offsetY": 1.0,
  "offsetZ": 0.5,
  "extra": 0.1
}
```

### 4.10 `TELEPORT`

Core type exists, but default map conversion is not registered in `RewardFactory`.

Builder example:

```java
AbstractReward reward = RewardBuilder.teleport()
    .world("world")
    .location(100.5, 64.0, -22.5)
    .rotation(90.0f, 0.0f)
    .build();
```

Representative JSON shape:

```json
{
  "type": "TELEPORT",
  "worldName": "world",
  "x": 100.5,
  "y": 64.0,
  "z": -22.5,
  "yaw": 90.0,
  "pitch": 0.0
}
```

### 4.11 `VANISHING_CHEST`

Core type exists, but default map conversion is not registered in `RewardFactory`.

Builder example:

```java
AbstractReward reward = RewardBuilder.vanishingChest()
    .addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 8))
    .addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 32))
    .durationSeconds(120)
    .dropItemsOnVanish(true)
    .build();
```

Representative JSON shape:

```json
{
  "type": "VANISHING_CHEST",
  "items": [
    { "type": "DIAMOND", "amount": 8 },
    { "type": "GOLD_INGOT", "amount": 32 }
  ],
  "durationTicks": 2400,
  "dropItemsOnVanish": true
}
```

## 5. One Combined Example (All Core Types)

This is a complete catalog-style example. In current source, `RewardFactory.fromMap(...)` can parse only `ITEM`, `CURRENCY`, `EXPERIENCE`, `COMMAND`, `COMPOSITE`, `CHOICE`, and `PERMISSION` unless you register additional converters.

```yml
rewards:
  item_reward:
    type: ITEM
    item:
      material: DIAMOND
      amount: 16

  currency_reward:
    type: CURRENCY
    currencyId: vault
    amount: 5000

  experience_reward:
    type: EXPERIENCE
    amount: 15
    experienceType: LEVELS

  command_reward:
    type: COMMAND
    command: "say {player} just claimed a reward!"
    executeAsPlayer: false
    delayTicks: 0

  composite_reward:
    type: COMPOSITE
    continueOnError: true
    rewards:
      - type: ITEM
        item:
          material: EMERALD
          amount: 8
      - type: CURRENCY
        currencyId: vault
        amount: 1000

  choice_reward:
    type: CHOICE
    minimumRequired: 1
    maximumRequired: 1
    allowMultipleSelections: false
    choices:
      - type: ITEM
        item:
          material: GOLDEN_APPLE
          amount: 4
      - type: EXPERIENCE
        amount: 200
        experienceType: POINTS

  permission_reward:
    type: PERMISSION
    permissions:
      - raindrop.cosmetic.sparkles
    durationSeconds: 3600
    temporary: true

  sound_reward:
    type: SOUND
    sound: ENTITY_PLAYER_LEVELUP
    volume: 1.0
    pitch: 1.1

  particle_reward:
    type: PARTICLE
    particle: HAPPY_VILLAGER
    count: 20
    offsetX: 0.5
    offsetY: 1.0
    offsetZ: 0.5
    extra: 0.1

  teleport_reward:
    type: TELEPORT
    worldName: world
    x: 250.5
    y: 72.0
    z: -110.5
    yaw: 180.0
    pitch: 0.0

  vanishing_chest_reward:
    type: VANISHING_CHEST
    items:
      - type: DIAMOND
        amount: 5
      - type: IRON_INGOT
        amount: 32
    durationTicks: 1200
    dropItemsOnVanish: true
```

## 6. RDQ Integration Review

Source:

- `RDQRewardSetup`
- `RDQRewardSectionAdapter`
- `RewardSection`
- `RankSystemFactory` reward parsing path
- Reward-related entities/listeners/distributors in RDQ

What RDQ does:

- Registers `RewardSection -> AbstractReward` adapter in `RewardFactory`.
- Parses configured rank rewards through `RewardFactory.fromSection(...)`.
- Stores and grants rewards through rank/perk/bounty entity workflows.

RDQ adapter-supported section types:

- `ITEM`, `CURRENCY`, `EXPERIENCE`, `COMMAND`, `COMPOSITE`, `CHOICE`, `PERMISSION`, `PERK`

RDQ mapping notes:

- `EXPERIENCE` uses section fields `experienceAmount` and `experienceType`.
- `PERK` maps to `PerkReward` with fields `perkIdentifier` and `autoEnable`.
- Adapter does not map `SOUND`, `PARTICLE`, `TELEPORT`, `VANISHING_CHEST`, or `QUEST`.

RDQ runtime notes:

- Bounty distribution strategies (`INSTANT`, `DROP`, `CHEST`, `VIRTUAL`) currently process only `ItemReward`; non-item reward types are skipped in those distributors.
- `QuestReward` exists in `com.raindropcentral.rdq.quest.reward`, but no reward registry/provider registration for it was found in source.

## 7. RDR Integration Review

Review result for current `RDR` module source:

- No usage of `com.raindropcentral.rplatform.reward.*`
- No `RewardFactory`/`RewardService` integration
- No reward setup/adapter implementation comparable to requirements integration

Current conclusion: RDR does not implement RPlatform reward-system integration in source.

## 8. RDS Integration Review

Review result for current `RDS` module source:

- No usage of `com.raindropcentral.rplatform.reward.*`
- No `RewardFactory`/`RewardService` integration
- No reward setup/adapter implementation comparable to requirements integration

Note:

- `RDS` has its own item/store model (`AbstractItem`, `ShopItem`) with reward-like terminology in description keys/messages, but this is separate from the RPlatform reward system.

Current conclusion: RDS does not implement RPlatform reward-system integration in source.

## 9. Extension Patterns

### 9.1 Add map-based config parsing for more reward types

Register converters in `RewardFactory`:

```java
RewardFactory.getInstance().registerConverter("SOUND", config -> {
    var sound = org.bukkit.Sound.valueOf(config.get("sound").toString());
    float volume = Float.parseFloat(config.getOrDefault("volume", "1.0").toString());
    float pitch = Float.parseFloat(config.getOrDefault("pitch", "1.0").toString());
    return new com.raindropcentral.rplatform.reward.impl.SoundReward(sound, volume, pitch);
});
```

### 9.2 Add section-based parsing

Implement `RewardSectionAdapter<T>` and register it:

```java
RewardFactory.getInstance().registerSectionAdapter(MyRewardSection.class, new MyRewardSectionAdapter());
```

### 9.3 Add validation

Register custom validators:

```java
RewardValidators.registerValidator(MyReward.class, reward -> ValidationResult.success());
```

### 9.4 Add lifecycle hooks

Register hooks in `LifecycleRegistry`:

```java
LifecycleRegistry.getInstance().registerHook(new RewardLifecycleHook() {
    @Override
    public boolean beforeGrant(Player player, AbstractReward reward) {
        return true;
    }
});
```

## 10. Known Implementation Caveats

Current source-level caveats to account for:

- `RewardFactory` default map converters do not include `SOUND`, `PARTICLE`, `TELEPORT`, `VANISHING_CHEST`.
- `AbstractReward` subtype mapping does not include `SOUND`.
- `RewardRegistry.configureObjectMapper(...)` is a no-op, so registry types are not currently auto-registered into `RewardParser`.
- `BuiltInRewardProvider.register()` and `unregister()` call back into `RewardRegistry.registerProvider/unregisterProvider`, which recursively calls provider register/unregister again.
- `RPlatform.initialize()` does not currently initialize built-in reward provider registration.
- `RewardConverter` old-format migration mapping does not include `"SoundReward" -> "SOUND"`.
- `ChoiceReward.grant(player)` does not grant rewards; use `grantChoices(...)`.
- `CommandReward`, `TeleportReward`, and `VanishingChestReward` scheduler calls use `Bukkit.getPluginManager().getPlugins()[0]` as task owner.
- RDQ custom reward types (`PERK`, `QUEST`) are not wired into `AbstractReward` subtype mapping, and no reward-provider registration for those types was found.
- RDQ `PerkReward` depends on static `PerkManagementService` injection via `PerkReward.setPerkManagementService(...)`; no invocation was found in RDQ source.
- RDQ bounty distributors derive stack sizes from `ItemReward.getItem()` (template amount `1`), which can under-represent multi-quantity `ItemReward` values during distribution.

## 11. Quick API Example (Programmatic)

```java
var pack = RewardBuilder.composite()
    .continueOnError(true)
    .add(RewardBuilder.item()
        .item(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1))
        .amount(10)
        .build())
    .add(RewardBuilder.currency().vault(1500).build())
    .add(RewardBuilder.experience().levels(5).build())
    .add(RewardBuilder.command()
        .command("say Reward granted to {player}")
        .asConsole()
        .delay(0)
        .build())
    .build();

RewardService.getInstance().grant(player, pack).thenAccept(success -> {
    if (!success) {
        player.sendMessage("Reward grant failed");
    }
});
```
