# Blue Crab LMS - Build Guide

## Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.8.7 or higher

### Basic Build Commands

```bash
# Standard build (8 seconds)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Compile only (quick syntax check)
mvn compile

# Run tests only
mvn test
```

### Output Location

```
Build artifacts are generated in:
target/BlueCrab-1.0.0.war    (130MB) ← Deploy this file
```

## Optimized Build (Recommended for Development)

### Install Maven Daemon (mvnd) - One-time Setup

```bash
# Download and install
wget https://github.com/apache/maven-mvnd/releases/download/1.0.3/maven-mvnd-1.0.3-linux-amd64.tar.gz
tar -xzf maven-mvnd-1.0.3-linux-amd64.tar.gz

# Add to PATH
echo 'export PATH="$HOME/maven-mvnd-1.0.3-linux-amd64/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# Optional: Create aliases
echo 'alias mb="mvnd clean package -DskipTests"' >> ~/.bashrc
echo 'alias mbt="mvnd clean package"' >> ~/.bashrc
source ~/.bashrc
```

### Use mvnd for Faster Builds

```bash
# First build: ~8 seconds (daemon startup)
mvnd clean package -DskipTests

# Subsequent builds: ~4.4 seconds (46% faster!)
mvnd clean package -DskipTests

# Or use alias
mb
```

## Build Performance

| Method | Time | Notes |
|--------|------|-------|
| `mvn` | 8.09s | Standard Maven |
| `mvnd` (first) | 8.55s | Daemon startup overhead |
| `mvnd` (warmed up) | 4.36s | **Recommended** |

**Daily time savings**: ~1-2 minutes per developer

## Deployment

### Deploy to Tomcat

```bash
# Copy WAR to Tomcat
cp target/BlueCrab-1.0.0.war /path/to/tomcat/webapps/

# Or rename to ROOT.war for root context
cp target/BlueCrab-1.0.0.war /path/to/tomcat/webapps/ROOT.war
```

### Run with Embedded Server

```bash
java -jar target/BlueCrab-1.0.0.war
```

## eGovFrame Compatibility

This project uses **eGovFrame Boot 4.3.0** (Spring Boot based):
- No special IDE required for building
- Standard Maven/Gradle project structure
- Build with any IDE or command line
- eGovFrame libraries included automatically via `pom.xml`

## Build Configuration

### Java Version

```xml
<!-- pom.xml -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### Skip Tests

```bash
# Maven
mvn clean package -DskipTests

# Maven Daemon
mvnd clean package -DskipTests
```

### Offline Mode

```bash
# Download dependencies once
mvn dependency:go-offline

# Build without checking for updates
mvn clean package -o
```

## Troubleshooting

### Build Fails - Out of Memory

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g"
mvn clean package
```

### Build Fails - Wrong Java Version

```bash
# Check Java version
java -version  # Should show Java 21

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### mvnd Daemon Issues

```bash
# Stop daemon and retry
mvnd --stop
mvnd clean package -DskipTests

# Check daemon status
mvnd --status
```

## CI/CD

For automated builds (Jenkins, GitHub Actions, GitLab CI), use standard Maven:

```yaml
# .gitlab-ci.yml example
build:
  stage: build
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.war
```

**Note**: mvnd is optimized for repeated local builds, not CI/CD.

## More Information

- [Detailed Build Optimization Guide](../../BUILD_OPTIMIZATION_GUIDE.md)
- [Java 21 Upgrade Summary](../../JAVA_UPGRADE_SUMMARY.md)
- [Project README](../../README.md)

## System Requirements

**Development Environment:**
- OS: Linux/Ubuntu (recommended), macOS, Windows
- Java: 21 or higher (LTS)
- Maven: 3.8.7 or higher
- RAM: 4GB minimum, 8GB recommended
- Disk: 500MB for dependencies

**Production Environment:**
- Java Runtime: 21 or higher
- Application Server: Tomcat 10+ or any Jakarta EE 9+ compatible server
- RAM: 2GB minimum, 4GB recommended

---

**Last Updated**: 2025-10-31
