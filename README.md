# YUI Compressor Maven Plugin

[![CI](https://github.com/codelibs/yuicompressor-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/codelibs/yuicompressor-maven-plugin/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.codelibs.maven/yuicompressor-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.codelibs.maven/yuicompressor-maven-plugin)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

Maven plugin to compress (minify/obfuscate/aggregate) JavaScript and CSS files using [YUI Compressor](http://yui.github.io/yuicompressor/).

This project is a fork of [net.alchim31.maven:yuicompressor-maven-plugin](https://github.com/davidB/yuicompressor-maven-plugin) maintained by the [CodeLibs Project](https://www.codelibs.org/) for continued development and modern Maven support.

## Features

- **JavaScript Compression**: Minification and obfuscation using YUI Compressor
- **CSS Compression**: Minification of CSS files
- **JSLint Integration**: Validate JavaScript files during build
- **File Aggregation**: Concatenate multiple files before or after compression
- **GZIP Support**: Automatically create gzipped versions
- **Incremental Builds**: IDE integration with M2E incremental build support
- **Selective Compression**: Skip already-minified files
- **Compression Statistics**: Optional reporting of compression ratios

## Requirements

- Maven 3.8.8 or later
- Java 11 or later

## Usage

### Basic Configuration

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codelibs.maven</groupId>
            <artifactId>yuicompressor-maven-plugin</artifactId>
            <version>2.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>compress</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Configuration Options

#### Common Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `encoding` | UTF-8 | File encoding |
| `suffix` | -min | Output filename suffix |
| `nosuffix` | false | Skip suffix addition |
| `linebreakpos` | -1 | Line break position |
| `force` | false | Force recompression |
| `gzip` | false | Create gzipped versions |
| `statistics` | true | Show compression statistics |

#### JavaScript-Specific Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `nocompress` | false | Skip compression (copy only) |
| `nomunge` | false | Minify only, no obfuscation |
| `preserveAllSemiColons` | false | Keep unnecessary semicolons |
| `disableOptimizations` | false | Disable micro optimizations |
| `jswarn` | true | Display JavaScript warnings |

### Examples

#### Compress with Custom Suffix

```xml
<configuration>
    <suffix>.compressed</suffix>
</configuration>
```

#### Create GZIP Versions

```xml
<configuration>
    <gzip>true</gzip>
    <level>9</level>
</configuration>
```

#### File Aggregation

```xml
<configuration>
    <aggregations>
        <aggregation>
            <output>${project.build.directory}/all.js</output>
            <includes>
                <include>file1.js</include>
                <include>file2.js</include>
            </includes>
            <insertNewLine>true</insertNewLine>
        </aggregation>
    </aggregations>
</configuration>
```

#### JSLint Validation

```xml
<execution>
    <goals>
        <goal>jslint</goal>
    </goals>
</execution>
```

## Build Instructions

### Prerequisites

- JDK 11 or later
- Maven 3.8.8 or later

### Build Commands

```bash
# Build the plugin
mvn clean install

# Run tests
mvn test

# Run integration tests
mvn verify

# Skip tests
mvn install -DskipTests

# Generate site documentation
mvn site
```

## Migration from net.alchim31.maven

If you're migrating from `net.alchim31.maven:yuicompressor-maven-plugin`, simply update your `pom.xml`:

```xml
<!-- Old -->
<groupId>net.alchim31.maven</groupId>
<artifactId>yuicompressor-maven-plugin</artifactId>
<version>1.5.1</version>

<!-- New -->
<groupId>org.codelibs.maven</groupId>
<artifactId>yuicompressor-maven-plugin</artifactId>
<version>2.0.0-SNAPSHOT</version>
```

All configuration options remain compatible.

## Issues

Found a bug or have a feature request? Please report it to the [issue tracker](https://github.com/codelibs/yuicompressor-maven-plugin/issues).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Credits

### Original Authors

- [David Bernard](https://github.com/davidB) - Original author
- [Piotr Kuczynski](https://github.com/pkuczynski) - Contributor

### CodeLibs Maintainers

- [Shinsuke Sugaya](https://github.com/shinsuke-sugaya) - Lead maintainer

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

The original project was in the public domain (Unlicense). This fork maintains compatibility while using the Apache License 2.0.

## Links

- [CodeLibs Project](https://www.codelibs.org/)
- [YUI Compressor](http://yui.github.io/yuicompressor/)
- [Original Project](https://github.com/davidB/yuicompressor-maven-plugin)
