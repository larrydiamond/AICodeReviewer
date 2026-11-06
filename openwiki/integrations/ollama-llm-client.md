---
type: integration
title: Ollama LLM Client and Helpers
description: How App connects to a local Ollama server through the simple-openai library, which models it names, how each chat helper builds requests and handles errors, and the PDF and directory utilities that run() does not use yet.
tags: [ollama, llm, simple-openai, integration, pdfbox]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Ollama LLM Client and Helpers

All LLM access is in `App` (`src/main/java/com/ldiamond/dli/App.java`). The app uses the `io.github.sashirestela:simple-openai` library against Ollama's OpenAI-compatible API. It has no other LLM provider and no abstraction layer.

## Client configuration

`App.run` builds a new `SimpleOpenAI` client on every run:

- `baseUrl("http://localhost:11434")`, the default local Ollama port. It is hardcoded.
- `apiKey`, `organizationId` and `projectId` are all set to the placeholder `"ollama"`. Ollama ignores them, but the OpenAI client builder requires values.

The client is passed explicitly to each helper method. It is not a Spring bean.

## Model catalog

The models are `static final String` constants on `App`:

| Constant | Ollama model | Used for |
|---|---|---|
| `gemma2` | `gemma2` | Classifier vote, code review, **judge** |
| `llama3` | `llama3.1` | Classifier vote |
| `llama32` | `llama3.2` | Classifier vote |
| `QWEN25CODER` | `qwen2.5-coder` | Code review |
| `FALCON3` | `falcon3` | Code review |
| `DEEPSEEKR1` | `deepseek-r1` | Defined, but its line in the reviewer list is commented out |

All five active models must be pulled into Ollama before you run the app (see [Build, Run, and Configuration](../operations/build-and-run.md)). The integration tests use a different model, `ornith-1.5:9b` (see [Testing Strategy](../testing/test-strategy.md)).

## Chat helper methods

Each helper builds a `ChatRequest` with `temperature(0.0)`, for output that is as deterministic as possible, and `maxCompletionTokens(-1)`, which means no explicit limit. Each one submits the request through `openAI.chatCompletions().create(...)`, blocks on `join()`, and returns `chatResponse.firstContent()`.

| Method | Messages sent | Error handling |
|---|---|---|
| `userMessageOnly(openAI, model, user)` | One user message | None. Exceptions propagate out of `run`. |
| `systemUserMessage(openAI, model, system, user)` | System and user message | None. Exceptions propagate. |
| `userImage(openAI, model, user, imageFile)` | User message with text and a Base64-encoded image | Catches `Exception`, logs at SEVERE, returns `""` |
| `userTextAttachment(openAI, model, system, user, attachment)` | System message, plus a user message with two text parts | Catches, logs, returns `""` |
| `userSystemImage(openAI, model, user, system, imageFile)` | System message, plus a user message with text and an image | Catches, logs, returns `""` |

The review pipeline uses only `userMessageOnly` and `systemUserMessage`. If Ollama is unreachable or a model is missing, the failure surfaces as an exception from `join()` and stops the run. The image and attachment helpers are prepared for multimodal or document input, but nothing calls them. Note that `userSystemImage` takes the user message *before* the system message.

## Input utilities (not wired in)

Nothing in the current flow calls these helpers:

- **`getTextFromPDF(filename)`** extracts plain text with PDFBox `PDFTextStripper`.
- **`getHtmlFromPDF(filename)`** converts a PDF to HTML with PDFBox `PDFText2HTML`.

  Both return `""` on `IOException`, after printing the stack trace.
- **`readDirectoryTreeToString(dir, extension)`** walks a directory tree breadth-first with a queue and concatenates every line of each file it finds. The `extension` parameter is ignored: the filter is hardcoded to `.java`. Lines are joined with an empty separator, so the newlines between them are lost. Each `DirectoryStream` is closed manually, not in try-with-resources, so an exception thrown during iteration leaks the stream.

These helpers are the main extension points for reviewing whole projects or non-code documents. To use one, wire it into `run` in place of the single-file read described in [Code Review Pipeline](../workflows/code-review-pipeline.md).
