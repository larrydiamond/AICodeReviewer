---
type: testing
title: Testing Strategy
description: The three testing layers in the AI Code Reviewer (a Spring context smoke test, live Ollama integration tests over buggy example fixtures, and PIT mutation testing) and how to run each one.
tags: [testing, junit, integration-tests, pitest, fixtures]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-18fdeaac7d12c7b7f20e3a74
    resource: repo://build.gradle
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
  - id: openwiki-source-0a2489ae2adc43c09ffe257e
    resource: repo://src/test/java/com/ldiamond/dli/DliApplicationTests.java
  - id: openwiki-source-b0c7c11181a24ccafcb059e2
    resource: repo://src/test/java/com/ldiamond/dli/examples/CodeReviewExamplesIT.java
  - id: openwiki-source-2b8c71082262ca2861a42c9c
    resource: repo://src/test/resources/examples/BookStoreProduct.output
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Testing Strategy

The project tests at three levels. Only the first runs in a plain `./gradlew build`.

## 1. Context smoke test (unit)

`DliApplicationTests` is a `@SpringBootTest` with `@ActiveProfiles("test")` and one empty `contextLoads()` test. The `App` runner is annotated `@Profile("!test")`, so it is **not** created under this profile. The test therefore checks that the Spring context wires up, and it needs no file argument and no Ollama server. It does not exercise any review logic.

## 2. Example integration tests (live LLM)

`CodeReviewExamplesIT` (`src/test/java/com/ldiamond/dli/examples/`) is tagged `@Tag("integration")`. Each test has a 120-second `@Timeout`. Run the tests with:

```bash
./gradlew integrationTest   # needs Ollama at localhost:11434 with the model below
```

How it works:

- It sends each fixture to **`ornith-1.5:9b`**, which is a different model from the production reviewers. The request uses the same system prompt and user-prompt prefix as `App`, with `temperature` 0.0 and `max_tokens` 2000.
- It calls `POST /v1/chat/completions` **directly** with `java.net.http.HttpClient` and Jackson, without going through simple-openai. A comment in the test explains why: simple-openai does not deserialize Ollama's `reasoning` field. When a reasoning model hits the token limit before it writes `content`, the analysis would be lost.
- The text that the test checks is `message.reasoning + " " + message.content`. It is lowercased, then checked twice: it must not start with "no problem", and it must contain a keyword for the known bug.
- It runs **one model per fixture**. The test does not exercise the multi-model vote or the judge step in `App` (see [Code Review Pipeline](../workflows/code-review-pipeline.md)).

| Fixture (`src/test/resources/examples/`) | Planted bug | Asserted keyword |
|---|---|---|
| `BookStoreProduct.java` | Constructor swaps `description` and `displayName` | `constructor` |
| `Circumference.java` | Uses `3.15` instead of π | `pi` |
| `Inefficientstringconcatenation.java` | `+=` string concatenation in a loop | `stringbuilder` |
| `PetStoreProduct.java` | Copy-paste: `displayName = description` | `constructor` |
| `Previousreference.java` | Inverted index check, `get(offset - 1)` on a negative index | `index` |
| `Usedthewrongcollection.java` | `TreeMap` where `HashMap` suffices | `hashmap` |

Each fixture has a matching `.output` file. These are recorded console logs from earlier full runs of `App`, made in January 2025 on Windows. They show the judge's reasoning and the chosen rewrite. They are documentation that the README links to. Tests do not read them, and they are not golden files.

Fixtures are loaded from the classpath under `/examples/`. A missing fixture raises `IOException("Fixture not found on classpath: ...")`.

## 3. Mutation testing (PIT)

```bash
./gradlew pitest   # report: build/reports/pitest/index.html
```

The PIT setup in `build.gradle` targets `com.ldiamond.dli.*`. It excludes test classes that match `*IT` and the `integration` JUnit group, so mutation runs never call Ollama. It uses one thread per available processor and writes HTML and XML reports without timestamped folders. The only unit test is the context smoke test, and `App` is disabled under the `test` profile. So expect almost all mutants in `App` to survive until someone adds unit tests that mock the `SimpleOpenAI` client.

## Adding tests

- **New example bug:** add `Foo.java` under `src/test/resources/examples/` and a test method that calls `review("Foo.java")` and asserts a keyword. You can also record a `Foo.output` and link it from the README.
- **Unit-testing pipeline logic:** the vote and judge logic sits inline in `App.run` and builds its own client, so a unit test cannot reach it yet. To test it, first extract that logic into methods that receive a `SimpleOpenAI` (or an interface) as a parameter.

Related: [Build, Run, and Configuration](../operations/build-and-run.md).
