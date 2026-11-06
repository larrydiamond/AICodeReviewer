---
type: operations
title: Build, Run, and Configuration
description: How to build the AI Code Reviewer with Gradle and a Java 17 toolchain, set up the Ollama models it needs, run the jar against a file, and how logging and configuration actually behave.
tags: [build, gradle, operations, configuration, logging]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-18fdeaac7d12c7b7f20e3a74
    resource: repo://build.gradle
  - id: openwiki-source-12eccab9eb2d86aae390f578
    resource: repo://settings.gradle
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
  - id: openwiki-source-ece093ad10b91f3da0b7e34f
    resource: repo://src/main/resources/application.properties
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Build, Run, and Configuration

## Prerequisites

1. **Java 17.** `build.gradle` pins a Java 17 toolchain. The foojay toolchain resolver plugin in `settings.gradle` lets the Gradle wrapper download a JDK 17 when one is missing. To run the built jar you need Java 17+ on your `PATH`.
2. **Ollama running locally** at `http://localhost:11434` (`ollama serve` or the desktop app). The URL is hardcoded. See [Ollama LLM Client and Helpers](../integrations/ollama-llm-client.md).
3. **Models pulled:** `gemma2`, `llama3.1`, `llama3.2`, `qwen2.5-coder`, `falcon3`. If a model is missing, a chat call throws an exception and the run aborts.

## Build

```bash
./gradlew build          # compiles and runs unit tests (excludes the "integration" tag)
```

The Spring Boot plugin (3.5.16) packages an executable jar at `build/libs/dli-0.1.0.jar`. The name comes from `rootProject.name = 'dli'` and `version = '0.1.0'`.

Key dependencies:

- `spring-boot-starter`: core context only, no web server.
- `io.github.sashirestela:simple-openai`: the OpenAI-compatible client used against Ollama.
- `org.apache.pdfbox:pdfbox` and `pdfbox-tools`: PDF text and HTML extraction helpers.
- Lombok, compile-only plus annotation processor, used for `@Log`.

## Run

```bash
java -jar build/libs/dli-0.1.0.jar path/to/File.java
```

- The **first non-option argument** is the file to review. If you give no argument, `getNonOptionArgs().get(0)` throws `IndexOutOfBoundsException`. If the path is not a regular file (for example, a directory), the app logs `Not a file` and exits.
- The classifier warm-up vote runs **before** the file check, so the three text models are always called. See [Code Review Pipeline](../workflows/code-review-pipeline.md).
- The results are log lines, not a structured report.

## Gradle tasks

| Task | What it does |
|---|---|
| `./gradlew test` | Runs JUnit 5 tests, excluding the `integration` tag |
| `./gradlew integrationTest` | Runs only `integration`-tagged tests. Needs a live Ollama. Ordered after `test`. |
| `./gradlew pitest` | Runs PIT mutation testing on `com.ldiamond.dli.*`. Excludes `*IT` and the `integration` group. HTML and XML reports go to `build/reports/pitest/` (no timestamped folders). |

Details: [Testing Strategy](../testing/test-strategy.md).

## Configuration and logging

- `application.properties` sets only `spring.application.name=dli`. The endpoint, model names, prompts and response thresholds are all constants in `App`, so changing them means changing code.
- `build.gradle` removes `spring-boot-starter-logging` from **every** configuration. Logback therefore never reaches the runtime classpath. `App` logs through Lombok `@Log`, which is `java.util.logging`, so the console shows the JUL default format (for example `Jan 28, 2025 10:03:38 PM com.ldiamond.dli.App run` / `INFO: ...`), as the recorded `.output` example files show.
- `src/main/resources/logback.xml` sets a debug-level console pattern. Because Logback is excluded, this file probably has no effect at runtime. Check this before relying on it.
