# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YUI Compressor Maven Plugin - compresses (minifies/obfuscates) JavaScript and CSS files using YUI Compressor. Fork of `net.alchim31.maven:yuicompressor-maven-plugin` maintained by CodeLibs Project.

## Build Commands

```bash
# Build and install locally
mvn clean install

# Run unit tests only
mvn test

# Run single test class
mvn test -Dtest=AggregationTest

# Run integration tests (located in src/it/)
mvn verify

# Skip all tests
mvn install -DskipTests

# Generate site documentation
mvn site
```

## Architecture

### Plugin Goals

| Goal | Mojo Class | Description |
|------|------------|-------------|
| `compress` | `YuiCompressorMojo` | Compresses JS/CSS files |
| `jslint` | `JSLintMojo` | Validates JS files with JSLint |

### Core Classes (org.codelibs.maven.yuicompressor)

- **MojoSupport** - Abstract base class for all mojos. Handles:
  - Directory scanning with include/exclude patterns
  - Source directory resolution (sourceDirectory, warSourceDirectory, webappDirectory, resources)
  - M2E incremental build integration via `BuildContext`
  - Template method pattern: `beforeProcess()` → `processFile()` → `afterProcess()`

- **YuiCompressorMojo** - Main compression mojo extending MojoSupport:
  - Compresses JS via `JavaScriptCompressor`, CSS via `CssCompressor`
  - Supports file aggregation (concatenation before/after compression)
  - GZIP output generation
  - Compression statistics reporting

- **Aggregation** - Represents file aggregation configuration. Combines multiple files into one.

- **SourceFile** - Wrapper for input files with path resolution utilities.

- **ErrorReporter4Mojo** - Bridges Mozilla Rhino's ErrorReporter to Maven's logging.

- **JSLintMojo** / **JSLintChecker** / **BasicRhinoShell** - JSLint validation infrastructure.

### Directory Structure

```
src/main/java/org/codelibs/maven/yuicompressor/  # Plugin source
src/test/java/                                     # Unit tests
src/it/                                            # Integration tests (maven-invoker-plugin)
  demo01/                                          # Basic compression test
  issue19/                                         # Regression test
```

### Integration Tests

Uses maven-invoker-plugin with Groovy validation scripts:
- `setup.groovy` - Pre-build setup
- `validate.groovy` - Post-build assertions

## Key Configuration Points

Default includes: `**/*.js`, `**/*.css`
Default suffix: `-min`
Encoding: UTF-8
