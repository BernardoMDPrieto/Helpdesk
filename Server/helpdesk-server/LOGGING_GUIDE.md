# SLF4J Logging Guide

## Quick Reference

### Using SLF4J in Your Classes

Add this to every class that needs logging:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void myMethod() {
        logger.info("This is an info message");
        logger.debug("This is a debug message");
        logger.warn("This is a warning message");
        logger.error("This is an error message", exception);
    }
}
```

### Log Levels

| Level | Use Case | Example |
|-------|----------|---------|
| **DEBUG** | Detailed flow information, intermediate steps | Token generation, template processing, lookup steps |
| **INFO** | Major operations, user actions | Login, user creation, email sending |
| **WARN** | Recoverable errors, unexpected conditions | Duplicate user, invalid token, user not found |
| **ERROR** | Exceptions, failures requiring attention | Database errors, email failures, validation errors |

### Common Patterns

#### Logging method entry/exit with parameters:
```java
logger.debug("Processing request for user ID: {}", userId);
```

#### Logging successful operations:
```java
logger.info("User created successfully with ID: {}", savedUser.getId());
```

#### Logging errors with exception:
```java
logger.error("Error fetching users", exception);
```

#### Conditional logging:
```java
if (logger.isDebugEnabled()) {
    logger.debug("User details: {}", user);  // Expensive operation
}
```

## Configuration

### Log Levels in application.properties

```properties
# Root logger level
logging.level.root=INFO

# Application package
logging.level.com.bduarte.helpdeskserver=DEBUG

# Spring Security
logging.level.org.springframework.security=DEBUG

# Spring Web
logging.level.org.springframework.web=DEBUG
```

### File Output

Logs are written to: `logs/application.log`

Rotation policy:
- Max file size: 10MB
- Retention: 30 days
- Format: `yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL package.Class - message`

## Environment-Specific Logging

### Development Profile (dev)
```bash
java -jar app.jar --spring.profiles.active=dev
```
- Log level: DEBUG
- Outputs to console and file

### Production Profile (prod)
```bash
java -jar app.jar --spring.profiles.active=prod
```
- Log level: INFO
- Outputs to console and file only

## Viewing Logs

### Real-time console output:
```bash
mvn spring-boot:run
```

### View log files:
```bash
# Last 50 lines
tail -n 50 logs/application.log

# Watch for new logs
tail -f logs/application.log

# Search for specific text
grep "ERROR" logs/application.log
grep "user@example.com" logs/application.log
```

## Best Practices

1. **Use appropriate levels**
   - Don't log everything at INFO level in production
   - Use DEBUG for detailed diagnostic information

2. **Include context**
   - Include user IDs, operation IDs, or timestamps
   - `logger.info("Created user: {}", email);` ✓
   - `logger.info("User created");` ✗

3. **Use placeholders**
   - `logger.info("User: {}, Age: {}", name, age)` ✓
   - `logger.info("User: " + name + ", Age: " + age)` ✗

4. **Log exceptions with context**
   - `logger.error("Failed to process user: {}", userId, exception)` ✓
   - `logger.error(exception);` ✗

5. **Performance consideration**
   - String concatenation happens even if log level filters it out
   - Use placeholder parameters: `logger.debug("Value: {}", expensiveCalculation())`
   - Check level first for very expensive operations

## Troubleshooting

### No logs appearing?
1. Check `logging.level.root` in application.properties
2. Verify logger class name matches: `LoggerFactory.getLogger(YourClass.class)`
3. Check if log file permissions allow writing to `logs/` directory

### Too many logs?
- Reduce `logging.level.com.bduarte.helpdeskserver` to INFO
- Disable Spring verbose logging:
  ```properties
  logging.level.org.springframework=INFO
  logging.level.org.springframework.security=INFO
  ```

### Log files growing too large?
- They automatically rotate at 10MB
- Check retention period in logback-spring.xml
- Old files are compressed as .gz files

## Files Reference

- **Configuration**: `src/main/resources/logback-spring.xml`
- **Properties**: `src/main/resources/application.properties`
- **Log output**: `logs/application.log`
