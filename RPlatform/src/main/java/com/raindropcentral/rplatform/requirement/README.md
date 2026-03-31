# RPlatform Requirements System (`com.raindropcentral.rplatform.requirement`)

Last verified from source: March 20, 2026.

This document is the implementation-backed guide for using the RPlatform requirements system across modules, with verified behavior from:

- `RPlatform` core requirement classes/factory/service/registry
- `RDQ` requirement setup + section adapter
- `RDR` storage store requirement integration
- `RDS` shop store requirement integration

## 1. What This System Is

The requirements system provides:

- Dynamic requirement type registration (`RequirementRegistry`, `PluginRequirementProvider`)
- Runtime checking/consumption/progress (`RequirementService`)
- Config-to-object conversion (`RequirementFactory`, `RequirementSectionAdapter`)
- JSON persistence and polymorphic parsing (`RequirementParser`, JPA converters)
- Lifecycle hooks, Bukkit events, metrics, and validators

Core type registrations come from [`CoreRequirementTypes.java`](./CoreRequirementTypes.java) and [`BuiltInRequirementProvider.java`](./BuiltInRequirementProvider.java):

| Type ID | Implementation Class |
|---|---|
| `ITEM` | `ItemRequirement` |
| `CURRENCY` | `CurrencyRequirement` |
| `EXPERIENCE_LEVEL` | `ExperienceLevelRequirement` |
| `PERMISSION` | `PermissionRequirement` |
| `LOCATION` | `LocationRequirement` |
| `PLAYTIME` | `PlaytimeRequirement` |
| `COMPOSITE` | `CompositeRequirement` |
| `CHOICE` | `ChoiceRequirement` |
| `TIME_BASED` | `TimedRequirement` |
| `PLUGIN` | `PluginRequirement` |

## 2. Required Initialization Order

If you use `RequirementParser`/DB converters, requirement types must be registered first.

### Recommended

Use `RPlatform.initialize()` first. It registers built-in requirement types via `BuiltInRequirementProvider.initialize()`.

```java
RPlatform platform = new RPlatform(plugin);
platform.initialize().thenRun(() -> {
    // safe to parse requirements here
});
```

### If registering custom requirement types after parser use

Call:

```java
RequirementParser.resetMapper();
```

so new types are included in polymorphic deserialization.

## 3. Runtime Flow

1. Build requirement objects from config maps (`RequirementFactory.fromMap`) or section adapters (`fromSection`).
2. Check requirements with `RequirementService.isMet(player, requirement)`.
3. Show progress with `RequirementService.calculateProgress(...)`.
4. Consume on success using `RequirementService.consume(...)` (or module-specific consumption wrappers).

`RequirementService` behavior:

- 30s per-player/per-requirement cache
- fires `RequirementCheckEvent`, `RequirementConsumeEvent`, `RequirementMetEvent`
- executes lifecycle hooks from `LifecycleRegistry`
- records metrics in `RequirementMetrics`

## 4. CoreRequirementTypes Reference + Examples

These examples are in the map format consumed by `RequirementFactory.fromMap(...)`.

### 4.1 `ITEM`

Required:

- `type: ITEM`
- `requiredItems` (or `items`) as a list of item maps

Optional:

- `consumeOnComplete` (default `true`)
- `exactMatch` (default `true`)
- `description`

```yml
type: ITEM
requiredItems:
  - type: DIAMOND
    amount: 16
  - type: NETHERITE_INGOT
    amount: 1
consumeOnComplete: true
exactMatch: true
description: requirement.item.upgrade
```

### 4.2 `CURRENCY`

Required:

- `type: CURRENCY`
- `currency`
- `amount` (positive)

Optional:

- `consumable` (default `false`)

```yml
type: CURRENCY
currency: vault
amount: 5000
consumable: true
```

### 4.3 `EXPERIENCE_LEVEL`

Required:

- `type: EXPERIENCE_LEVEL`

Optional:

- `requiredLevel` (default `1`)
- `experienceType` (`LEVEL` or `POINTS`, default `LEVEL`)
- `consumeOnComplete` (default `true`)
- `description`

```yml
type: EXPERIENCE_LEVEL
requiredLevel: 30
experienceType: LEVEL
consumeOnComplete: true
description: requirement.exp.master
```

### 4.4 `PERMISSION`

Required:

- `type: PERMISSION`
- `requiredPermissions` list

Optional:

- `permissionMode` (`ALL`, `ANY`, `MINIMUM`; default `ALL`)
- `checkNegated` (default `false`)
- `description`

```yml
type: PERMISSION
requiredPermissions:
  - raindrop.rank.warrior
  - raindrop.rank.guardian
permissionMode: ANY
checkNegated: false
description: requirement.permission.path
```

### 4.5 `LOCATION`

At least one of:

- `requiredWorld`
- `requiredRegion`
- `requiredCoordinates` (`x`,`y`,`z`)

Optional:

- `requiredDistance`
- `description`

```yml
type: LOCATION
requiredWorld: world
requiredCoordinates:
  x: 100.0
  y: 64.0
  z: -200.0
requiredDistance: 12.0
description: requirement.location.temple
```

### 4.6 `PLAYTIME`

Required:

- `type: PLAYTIME`
- either `requiredPlaytimeSeconds` (total mode) or `worldPlaytimeRequirements` (world mode)

Optional:

- `description`

```yml
type: PLAYTIME
requiredPlaytimeSeconds: 72000
description: requirement.playtime.veteran
```

World-specific variant:

```yml
type: PLAYTIME
worldPlaytimeRequirements:
  world: 36000
  world_nether: 7200
description: requirement.playtime.multiverse
```

### 4.7 `COMPOSITE`

Required:

- `type: COMPOSITE`
- `requirements` list of nested requirement maps

Optional:

- `operator` (`AND`, `OR`, `MINIMUM`; default `AND`)
- `minimumRequired` (for `MINIMUM`)
- `allowPartialProgress` (default `true`)
- `description`

```yml
type: COMPOSITE
operator: MINIMUM
minimumRequired: 2
allowPartialProgress: true
requirements:
  - type: CURRENCY
    currency: vault
    amount: 2000
    consumable: true
  - type: EXPERIENCE_LEVEL
    requiredLevel: 25
    experienceType: LEVEL
  - type: PERMISSION
    requiredPermissions:
      - raindrop.guild.member
```

### 4.8 `CHOICE`

Required:

- `type: CHOICE`
- `choices` list of nested requirement maps

Optional:

- `minimumChoicesRequired` (default `1`)
- `mutuallyExclusive` (default `false`)
- `allowChoiceChange` (default `true`)
- `description`

```yml
type: CHOICE
minimumChoicesRequired: 1
mutuallyExclusive: false
allowChoiceChange: true
choices:
  - type: ITEM
    requiredItems:
      - type: DIAMOND_BLOCK
        amount: 8
    consumeOnComplete: true
  - type: CURRENCY
    currency: vault
    amount: 15000
    consumable: true
description: requirement.choice.buy_or_collect
```

### 4.9 `TIME_BASED`

Required:

- `type: TIME_BASED`
- `delegate` nested requirement
- `timeLimitMillis`

Optional:

- `autoStart` (default `true`)
- `description`

```yml
type: TIME_BASED
timeLimitMillis: 600000
autoStart: true
delegate:
  type: ITEM
  requiredItems:
    - type: BLAZE_ROD
      amount: 12
  consumeOnComplete: true
description: requirement.timed.blaze
```

### 4.10 `PLUGIN`

Required:

- `type: PLUGIN`
- `values` map (or compatible legacy aliases)
- `plugin` recommended (falls back to `"auto"` if omitted)

Optional:

- `category` (`SKILLS`, `JOBS`, etc.)
- `consumable` (default `false`)
- `description`

```yml
type: PLUGIN
plugin: ecoskills
category: SKILLS
values:
  mining: 50
  combat: 30
consumable: false
description: requirement.skills.combat_path
```

Auto-detect skills bridge:

```yml
type: PLUGIN
plugin: auto
category: SKILLS
values:
  mining: 50
```

## 5. One Combined Example (All Core Types)

```yml
requirements:
  item_req:
    type: ITEM
    requiredItems:
      - type: DIAMOND
        amount: 16
    consumeOnComplete: true
    exactMatch: true

  currency_req:
    type: CURRENCY
    currency: vault
    amount: 2500
    consumable: true

  exp_req:
    type: EXPERIENCE_LEVEL
    requiredLevel: 20
    experienceType: LEVEL
    consumeOnComplete: true

  permission_req:
    type: PERMISSION
    requiredPermissions:
      - raindrop.rank.member
      - raindrop.rank.veteran
    permissionMode: ANY

  location_req:
    type: LOCATION
    requiredWorld: world
    requiredCoordinates:
      x: 0
      y: 64
      z: 0
    requiredDistance: 20

  playtime_req:
    type: PLAYTIME
    requiredPlaytimeSeconds: 14400

  composite_req:
    type: COMPOSITE
    operator: MINIMUM
    minimumRequired: 2
    requirements:
      - type: CURRENCY
        currency: vault
        amount: 3000
        consumable: true
      - type: EXPERIENCE_LEVEL
        requiredLevel: 25
        experienceType: LEVEL
      - type: PERMISSION
        requiredPermissions:
          - raindrop.guild.member

  choice_req:
    type: CHOICE
    minimumChoicesRequired: 1
    choices:
      - type: ITEM
        requiredItems:
          - type: EMERALD
            amount: 64
      - type: CURRENCY
        currency: vault
        amount: 10000
        consumable: true

  timed_req:
    type: TIME_BASED
    timeLimitMillis: 300000
    delegate:
      type: ITEM
      requiredItems:
        - type: BLAZE_POWDER
          amount: 24
      consumeOnComplete: true

  plugin_req:
    type: PLUGIN
    plugin: jobsreborn
    category: JOBS
    values:
      miner: 10
```

## 6. RDQ Integration Review

Source:

- `RDQRequirementSetup`
- `RDQRequirementSectionAdapter`
- `RDQRequirementValidators`

What RDQ does:

- registers `BaseRequirementSection -> AbstractRequirement` adapter in `RequirementFactory`
- registers validators for `ITEM`, `EXPERIENCE_LEVEL`, `PERMISSION`, `COMPOSITE`, `CHOICE`
- registers lifecycle logging hook

Supported RDQ section types in active adapter:

- `ITEM`, `CURRENCY`, `EXPERIENCE_LEVEL`, `PERMISSION`, `LOCATION`, `PLAYTIME`, `COMPOSITE`, `CHOICE`, `SKILLS`, `JOBS`, `PLUGIN`

RDQ mapping notes:

- `SKILLS` and `JOBS` are converted to core `PLUGIN` requirements with category `SKILLS`/`JOBS`.
- `PLUGIN` in RDQ currently only accepts skill/job-shaped data.
- `CURRENCY` section can contain multiple currencies, but adapter currently uses the first entry only.
- `PERMISSION` section maps `requireAll` to `ALL`/`ANY`.
- `TIME_BASED` conversion exists in legacy `BaseRequirementSectionAdapter` but is not in `RDQRequirementSectionAdapter` (the adapter currently registered by setup).

## 7. RDR Integration Review

Source:

- `RDRRequirementSetup`
- `StoreRequirementSection`
- `StorageStorePricingSupport`
- `StorageRequirementConfigSupport`

What RDR does:

- registers section adapter for `StoreRequirementSection`
- adapter converts section to normalized requirement map and calls `RequirementFactory.fromMap(...)`

RDR normalization highlights (`StoreRequirementSection.toRequirementMap()`):

- flattens nested keys like `itemRequirement`, `currencyRequirement`, etc.
- supports aliases (`world` -> `requiredWorld`, `requiredType` -> `experienceType`, etc.)
- normalizes timed forms (`timeConstraintSeconds`, `timeLimitMinutes/hours/days`) to `timeLimitMillis`
- normalizes plugin aliases (`pluginId`, `integrationId`, `skillPlugin`, `jobPlugin`, `skills`, `jobs`, `key/value`)

RDR runtime behavior differences:

- store purchase uses requirement checks from RPlatform, but currency amounts can be discounted before purchase consumption.
- banking progress is supported for currency/item requirements.
- currency/item consumption in store purchase path is effectively enforced by store logic, independent of `consumable`/`consumeOnComplete` flags.

## 8. RDS Integration Review

Source:

- `StoreRequirementSection`
- `ShopStorePricingSupport`
- `ShopRequirementConfigSupport`

What RDS does:

- does not use a dedicated `RequirementSetup` class; it directly converts normalized maps via `RequirementFactory.fromMap(...)` in store pricing support.
- uses the same normalization strategy as RDR for requirement map aliases.
- `ShopRequirementConfigSupport` writes default `PLUGIN` requirements for skills/jobs into tiered shop config.

RDS runtime behavior differences:

- banking for currency/item progress is only supported when the requirement is consumable (`CurrencyRequirement.isConsumable()` / `ItemRequirement.isConsumeOnComplete()`).
- non-consumable currency/item requirements are treated as checks only during shop purchase consumption path.

## 9. Extension Patterns

### 9.1 Register custom requirement types

Implement `PluginRequirementProvider`, return `Map<String, RequirementType>`, then register with `RequirementRegistry`.

### 9.2 Add custom config section mapping

Register a `RequirementSectionAdapter<T>` in `RequirementFactory`.

### 9.3 Add validation

Register validators with `ValidationRegistry` keyed by type ID.

### 9.4 Add lifecycle hooks

Register `RequirementLifecycleHook` with `LifecycleRegistry`.

## 10. Known Implementation Caveats

These are current source-level behaviors worth accounting for when writing configs:

- `RequirementFactory` map conversion for `PERMISSION` does not pass `minimumRequired` into builder mode today.
- `RequirementFactory` map conversion for `CHOICE` currently ignores `maximumRequired` and `allowPartialProgress` fields.
- `RequirementFactory` map conversion for `COMPOSITE` currently ignores `maximumRequired`.
- `RequirementFactory` map conversion for `PLAYTIME` infers world mode from `worldPlaytimeRequirements`; explicit `useTotalPlaytime` is not directly consumed in that path.
- `RequirementParser` throws if registry has no types; ensure `RPlatform.initialize()` (or equivalent type registration) finished first.

## 11. Quick API Example (Programmatic)

```java
AbstractRequirement requirement = RequirementBuilder.composite()
    .minimum(2)
    .add(RequirementBuilder.currency().currency("vault").amount(1000).consumable(true).build())
    .add(RequirementBuilder.experience().level(15).points().consumeOnComplete(false).build())
    .add(RequirementBuilder.permission().permission("raindrop.rank.member").any().build())
    .description("requirement.composite.entry")
    .build();

RequirementService service = RequirementService.getInstance();
boolean met = service.isMet(player, requirement);
double progress = service.calculateProgress(player, requirement);
if (met) {
    service.consume(player, requirement);
}
```

