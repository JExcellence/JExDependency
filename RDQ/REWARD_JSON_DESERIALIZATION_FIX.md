# Reward JSON Deserialization Fix

## Problem

When clicking "View All Rewards" in the rank rewards view, the following error occurred:

```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: 
Unrecognized field "rewardCount" (class com.raindropcentral.rplatform.reward.impl.CompositeReward), 
not marked as ignorable (2 known properties: "continueOnError", "rewards"])
```

## Root Cause

The database contains old reward JSON data that was serialized with a `rewardCount` field. This field was likely stored when the reward system had a different structure. The current `CompositeReward` class doesn't have `rewardCount` as a serializable property - it only has a getter method that calculates the count from the `rewards` list.

When Jackson tries to deserialize the old JSON, it encounters the `rewardCount` field and throws an exception because it doesn't know how to handle it.

## Solution

Added `ignoreUnknown = true` to the `@JsonIgnoreProperties` annotation on the `AbstractReward` base class. This tells Jackson to ignore any unknown fields during deserialization, allowing the system to handle schema evolution gracefully.

### Changes Made

**File: `RPlatform/src/main/java/com/raindropcentral/rplatform/reward/AbstractReward.java`**

```java
// Before
@JsonIgnoreProperties(value = {"typeId", "estimatedValue", "descriptionKey"}, allowGetters = true)
public abstract non-sealed class AbstractReward implements Reward {

// After
@JsonIgnoreProperties(value = {"typeId", "estimatedValue", "descriptionKey"}, allowGetters = true, ignoreUnknown = true)
public abstract non-sealed class AbstractReward implements Reward {
```

## Benefits

1. **Backward Compatibility**: Old reward JSON data with extra fields can now be deserialized without errors
2. **Forward Compatibility**: Future schema changes won't break existing data
3. **Applies to All Rewards**: Since the annotation is on the base class, all reward types inherit this behavior
4. **No Data Migration Required**: Existing database records don't need to be updated

## Testing

- Compiled successfully: `./gradlew RPlatform:compileJava`
- RDQ module compiles: `./gradlew RDQ:rdq-common:compileJava`
- The "View All Rewards" button should now work without throwing deserialization errors

## Related Files

- `RPlatform/src/main/java/com/raindropcentral/rplatform/reward/AbstractReward.java` - Base reward class with fix
- `RPlatform/src/main/java/com/raindropcentral/rplatform/reward/impl/CompositeReward.java` - Affected reward type
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/view/ranks/RankRewardsDetailView.java` - View that triggered the error

## Notes

The `rewardCount` field in the database is redundant since it can be calculated from the `rewards` list size. The system now correctly ignores this field during deserialization and calculates the count dynamically using the `getRewardCount()` method.
