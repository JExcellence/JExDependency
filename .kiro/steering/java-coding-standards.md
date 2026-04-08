# Java Coding Standards

## Import Standards

### Always Use Explicit Imports
- NEVER use `java.util.Map.of()` or similar static method calls without importing
- ALWAYS add proper imports for all classes and static methods used

```java
// ❌ Bad: Using fully qualified names
java.util.Map<String, Object> map = java.util.Map.of("key", "value");

// ✅ Good: Proper imports
import java.util.Map;

Map<String, Object> map = Map.of("key", "value");
```

## Async Repository Methods

### Use Async Methods for Database Operations
- ALWAYS use `findByAttributesAsync()` instead of `findByAttributes()` for async operations
- ALWAYS use `findAllAsync()` instead of `findAll()` for async operations
- Repository methods ending with `Async` return `CompletableFuture`

```java
// ❌ Bad: Using synchronous method in async context
return repository.findByAttributes(Map.of("key", "value"))
    .thenCompose(results -> ...);

// ✅ Good: Using async method
return repository.findByAttributesAsync(Map.of("key", "value"))
    .thenCompose(results -> ...);
```

## Best Practices

1. Always import classes explicitly
2. Use async repository methods for non-blocking operations
3. Use synchronous methods only when blocking is acceptable
4. Prefer `CompletableFuture` for async operations
