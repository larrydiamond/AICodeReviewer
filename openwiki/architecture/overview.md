---
type: architecture
title: Architecture Overview
description: System-level structure of the AI Code Reviewer, a Spring Boot command-line app whose single ApplicationRunner sends a source file to several locally hosted Ollama models and picks the best review.
tags: [architecture, spring-boot, ollama, calm, cli]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-18fdeaac7d12c7b7f20e3a74
    resource: repo://build.gradle
  - id: openwiki-source-5332caa67e8393f94a11deae
    resource: repo://calm.json
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
  - id: openwiki-source-824c77dd761fc26dee2dd657
    resource: repo://src/main/java/com/ldiamond/dli/DliApplication.java
  - id: openwiki-source-ece093ad10b91f3da0b7e34f
    resource: repo://src/main/resources/application.properties
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Architecture Overview

The AI Code Reviewer (Gradle project `dli`, package `com.ldiamond.dli`) is a small command-line tool. It reads one source file, asks several local LLMs to rewrite it to fix bugs, security problems and performance problems, then asks another LLM to pick the best rewrite. Every model call goes to an Ollama server on the same machine.

## Components

| Component | Role |
|---|---|
| `DliApplication` | Spring Boot entry point (`@SpringBootApplication`). Its `main` calls `SpringApplication.run`. It has no web layer, because the only starter is `spring-boot-starter`. |
| `App` | The only application bean. It is a `@Component` that implements `ApplicationRunner`, so Spring calls `App.run(ApplicationArguments)` once the context has started. All of the review logic, the LLM client setup and the helper utilities live in this class. |
| Ollama server | An external process at `http://localhost:11434`. The app reaches it through its OpenAI-compatible chat completions API, using the `simple-openai` library. |

There are no repositories, services, controllers or configuration classes. `application.properties` sets only `spring.application.name=dli`.

## Runtime shape

1. The JVM starts `DliApplication.main`, and Spring Boot builds a context that contains `App`.
2. Spring calls `App.run` with the command-line arguments. The first non-option argument is the path of the file to review.
3. `App` builds a `SimpleOpenAI` client that points at Ollama. It runs a warm-up classifier vote, then the code review and the judge step, and logs the results through `java.util.logging` (Lombok `@Log`).
4. When `run` returns, the application exits. There is no server loop.

See [Code Review Pipeline](../workflows/code-review-pipeline.md) for the full control flow and [Ollama LLM Client and Helpers](../integrations/ollama-llm-client.md) for the client details.

## Profile gating

`App` carries `@Profile("!test")`. When the `test` profile is active (as in `DliApplicationTests`), the runner bean is not registered. The Spring context can then load in unit tests without a file argument and without a running Ollama server.

## CALM architecture model

`calm.json` describes the system in FINOS CALM 1.3. It has two nodes, `ai-code-reviewer` (a system) and `ollama-server` (a service hosting gemma2, llama3.1, llama3.2, qwen2.5-coder and falcon3), and one HTTP `connects` relationship from the reviewer to Ollama. This matches the code: Ollama is the only external dependency.

## Design consequences

- **Local only, fixed endpoint.** The Ollama URL and the model names are compile-time constants. The app has no configuration properties or environment variables for them.
- **Sequential, blocking calls.** Every model request is made in turn and waits with `CompletableFuture.join()`. A source comment marks running the models in parallel threads as future work.
- **Single-file scope.** `run` reviews exactly one file. A recursive directory reader exists (`readDirectoryTreeToString`), but `run` never calls it.

Related: [Build, Run, and Configuration](../operations/build-and-run.md).
