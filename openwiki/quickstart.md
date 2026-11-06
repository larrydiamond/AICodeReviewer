---
type: quickstart
title: Quickstart
description: Orientation for the AI Code Reviewer repository. Covers what it does, the commands to build, run and test it, where the logic lives, and which wiki page to read for each kind of task.
tags: [quickstart, overview, navigation]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
  - id: openwiki-source-824c77dd761fc26dee2dd657
    resource: repo://src/main/java/com/ldiamond/dli/DliApplication.java
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Quickstart

**AI Code Reviewer** (Gradle project `dli`) is a Spring Boot command-line app. It sends one source file to several LLMs hosted locally in Ollama and asks each to rewrite the file to fix bugs, security problems and performance problems. A judge model then picks the best rewrite, and the app logs it.

## In 30 seconds

- The logic lives almost entirely in **one class**: `src/main/java/com/ldiamond/dli/App.java`. It is an `ApplicationRunner`. `DliApplication.java` is only the Spring Boot `main`.
- **External dependency:** Ollama at `http://localhost:11434` (hardcoded). It needs the models `gemma2`, `llama3.1`, `llama3.2`, `qwen2.5-coder` and `falcon3`.
- **Configuration:** none to speak of. Models, prompts and the endpoint are constants in `App`.

## Commands

```bash
./gradlew build                                  # Java 17 toolchain; unit tests only
java -jar build/libs/dli-0.1.0.jar path/File.java # review one file (Ollama must be running)
./gradlew integrationTest                        # live-LLM example tests (needs ornith-1.5:9b in Ollama)
./gradlew pitest                                 # mutation testing → build/reports/pitest/index.html
```

## Where to go next

| If you want to… | Read |
|---|---|
| Understand the system's shape, Spring wiring and the CALM model | [Architecture Overview](architecture/overview.md) |
| Follow what happens to a file: the vote, the reviewers, the filter and the judge | [Code Review Pipeline](workflows/code-review-pipeline.md) |
| Change models, prompts, the endpoint or request settings, or use the PDF, image or directory helpers | [Ollama LLM Client and Helpers](integrations/ollama-llm-client.md) |
| Build, run, troubleshoot startup, or understand logging and config | [Build, Run, and Configuration](operations/build-and-run.md) |
| Add an example bug, run integration tests, or read PIT results | [Testing Strategy](testing/test-strategy.md) |

## Gotchas

- If you run without a file argument, the app throws `IndexOutOfBoundsException`. If you pass a directory, it logs `Not a file`.
- All three classifier models are called on every run, before the file is checked.
- The unit test suite never exercises `App`, because `@Profile("!test")` turns it off under tests. Only the integration tests call a real model.
