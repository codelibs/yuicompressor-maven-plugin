# YUI Compressor Maven Plugin

[![CI](https://github.com/codelibs/yuicompressor-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/codelibs/yuicompressor-maven-plugin/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codelibs.maven/yuicompressor-maven-plugin?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codelibs.maven/yuicompressor-maven-plugin)
[![License](https://img.shields.io/badge/license-Unlicense-blue)](https://unlicense.org/)

Maven plugin that compresses (minifies, obfuscates and aggregates) JavaScript and CSS
files using [YUI Compressor](https://github.com/codelibs/yuicompressor), and validates
JavaScript files with JSLint.

This project is a fork of
[net.alchim31.maven:yuicompressor-maven-plugin](https://github.com/davidB/yuicompressor-maven-plugin),
maintained by the [CodeLibs Project](https://www.codelibs.org/) for continued
development and modern Maven support. It is published as
`org.codelibs.maven:yuicompressor-maven-plugin` and builds on
`org.codelibs:yuicompressor` 2.4.11.

## Requirements

- Java 11 or later
- Maven 3.8.1 or later

## Getting Started

Add the plugin to the `build` section of your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codelibs.maven</groupId>
      <artifactId>yuicompressor-maven-plugin</artifactId>
      <version>2.0.1</version>
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

By default the `compress` goal picks up every `**/*.js` and `**/*.css` file under
`src/main/js`, the project resource directories and `src/main/webapp`, and writes the
compressed copy into the matching output directory (`target/classes`, or the exploded
web application for `src/main/webapp`) with a `-min` suffix: `app.js` becomes
`app-min.js`.

## Goals

| Goal | Default phase | Description |
|------|---------------|-------------|
| `compress` | `process-resources` | Compresses JavaScript and CSS files, and optionally aggregates and gzips them. |
| `jslint` | `process-resources` | Checks JavaScript files with JSLint. |

Both goals are thread-safe and support incremental builds in Eclipse through the
m2e `BuildContext` API.

## Configuration

### Source selection

These parameters are shared by both goals.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `sourceDirectory` | `${project.build.sourceDirectory}/../js` | Additional JavaScript source directory, for projects laid out as `src/main/js`. |
| `warSourceDirectory` | `${basedir}/src/main/webapp` | Web application source directory. |
| `webappDirectory` | `${project.build.directory}/${project.build.finalName}` | Directory the web application is built into. |
| `outputDirectory` | `${project.build.outputDirectory}` | Directory the processed files are written to. |
| `includes` | `**/*.js`, `**/*.css` (`compress`); `**/*.js` (`jslint`) | Include patterns. Replaces the defaults when set. |
| `excludes` | none | Additional exclude patterns. |
| `excludeResources` | `false` | Skip files under the project resource directories. |
| `excludeWarSourceDirectory` | `false` | Skip files under `warSourceDirectory`. |
| `useProcessedResources` | `false` | Read from the processed resources instead of the source resources. |
| `skip` | `false` | Skip execution entirely (`maven.yuicompressor.skip`). |
| `jswarn` | `true` | Report possible problems found in JavaScript (`maven.yuicompressor.jswarn`). |
| `failOnWarning` | `false` | Fail the build on warnings. Implies `jswarn` (`maven.yuicompressor.failOnWarning`). |

### `compress` goal

| Parameter | Default | Description |
|-----------|---------|-------------|
| `encoding` | `UTF-8` | Character set used to read the input files (`file.encoding`). |
| `suffix` | `-min` | Suffix appended to the output file name (`maven.yuicompressor.suffix`). |
| `nosuffix` | `false` | Write the output over the same file name, with no suffix (`maven.yuicompressor.nosuffix`). |
| `linebreakpos` | `-1` | Insert a line break after the given column. `-1` never breaks (`maven.yuicompressor.linebreakpos`). |
| `force` | `false` | Compress even when the output is newer than the source (`maven.yuicompressor.force`). |
| `gzip` | `false` | Also write a `.gz` copy of each compressed file (`maven.yuicompressor.gzip`). |
| `level` | `9` | GZIP compression level, 0-9 (`maven.yuicompressor.level`). |
| `statistics` | `true` | Log the compression ratio of each file and the total (`maven.yuicompressor.statistics`). |
| `useSmallestFile` | `true` | Keep the original when compression makes the file larger (`maven.yuicompressor.useSmallestFile`). |
| `aggregations` | none | Aggregations to run. See [File aggregation](#file-aggregation). |
| `preProcessAggregates` | `false` | Aggregate before compressing instead of after (`maven.yuicompressor.preProcessAggregates`). |

JavaScript-only parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `nocompress` | `false` | Copy the file without compressing it (`maven.yuicompressor.nocompress`). |
| `nomunge` | `false` | Minify only; do not obfuscate local symbols (`maven.yuicompressor.nomunge`). |
| `preserveAllSemiColons` | `false` | Accepted and ignored by YUI Compressor 2.4.11 (`maven.yuicompressor.preserveAllSemiColons`). |
| `disableOptimizations` | `false` | Accepted and ignored by YUI Compressor 2.4.11 (`maven.yuicompressor.disableOptimizations`). |

## Examples

### Overwrite the source files

```xml
<configuration>
  <nosuffix>true</nosuffix>
</configuration>
```

### Write gzipped copies

```xml
<configuration>
  <gzip>true</gzip>
  <level>9</level>
</configuration>
```

### Restrict the files to compress

```xml
<configuration>
  <includes>
    <include>**/*.js</include>
  </includes>
  <excludes>
    <exclude>**/vendor/**</exclude>
    <exclude>**/*-min.js</exclude>
  </excludes>
</configuration>
```

### File aggregation

```xml
<configuration>
  <aggregations>
    <aggregation>
      <output>${project.build.directory}/${project.build.finalName}/js/all.js</output>
      <includes>
        <include>**/jquery.js</include>
        <include>**/app.js</include>
      </includes>
      <insertNewLine>true</insertNewLine>
    </aggregation>
  </aggregations>
</configuration>
```

Each `aggregation` accepts `inputDir`, `output`, `includes`, `excludes`,
`removeIncluded`, `insertNewLine`, `insertFileHeader`, `fixLastSemicolon` and
`autoExcludeWildcards`. Includes are concatenated in the order they are listed.

### JSLint validation

```xml
<executions>
  <execution>
    <id>jslint</id>
    <goals>
      <goal>jslint</goal>
    </goals>
  </execution>
</executions>
```

## Migrating from net.alchim31.maven

Change the `groupId` and `version`; the `artifactId`, goals and configuration
parameters are unchanged.

```xml
<!-- Before -->
<groupId>net.alchim31.maven</groupId>
<artifactId>yuicompressor-maven-plugin</artifactId>
<version>1.5.1</version>

<!-- After -->
<groupId>org.codelibs.maven</groupId>
<artifactId>yuicompressor-maven-plugin</artifactId>
<version>2.0.1</version>
```

## Building from Source

```bash
# Build, run unit tests and install into the local repository
mvn clean install

# Unit tests only
mvn test

# Unit and integration tests (src/it, run with maven-invoker-plugin)
mvn verify

# Build without tests
mvn install -DskipTests
```

## Contributing

Bug reports and feature requests belong in the
[issue tracker](https://github.com/codelibs/yuicompressor-maven-plugin/issues).
Pull requests are welcome; please make sure `mvn verify` passes before opening one.

## License

Released into the public domain under [The Unlicense](https://unlicense.org/).
See [LICENSE.txt](LICENSE.txt).

## Credits

- [David Bernard](https://github.com/davidB) - original author
- [Piotr Kuczynski](https://github.com/pkuczynski) - contributor
- Maintained by the [CodeLibs Project](https://www.codelibs.org/)
