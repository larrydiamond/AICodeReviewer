---
type: workflow
title: Code Review Pipeline
description: Step-by-step control flow of App.run, covering the classifier majority-vote warm-up, the multi-model rewrite of the target file, response filtering, LLM-as-judge selection with substring fallback, and the log lines it emits.
tags: [workflow, code-review, llm-as-judge, majority-vote, pipeline]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-06T02:25:40.585Z
sources:
  - id: openwiki-source-c07c5ad17c5608c859af1d67
    resource: repo://src/main/java/com/ldiamond/dli/App.java
  - id: openwiki-source-2b8c71082262ca2861a42c9c
    resource: repo://src/test/resources/examples/BookStoreProduct.output
  - id: openwiki-source-9b57b9edc47286c69544e594
    resource: repo://src/test/resources/examples/Circumference.output
generated: { by: "claude-code", at: "2026-11-06T02:25:40.585Z" }
---

# Code Review Pipeline

The whole review runs inside `App.run(ApplicationArguments)` (`src/main/java/com/ldiamond/dli/App.java`). Every LLM call is a blocking, sequential request to Ollama. The helpers are described in [Ollama LLM Client and Helpers](../integrations/ollama-llm-client.md).

```
args ─► build SimpleOpenAI ─► [1] classifier vote (3 text models)
                           └► [2] read file ─► [3] reviewer rewrites (3 coding models)
                                              ─► [4] filter ─► [5] judge (gemma2) ─► [6] log best
```

## 1. Classifier majority vote (warm-up / demo)

Each of `gemma2`, `llama3.1` and `llama3.2` gets the same fixed question ("Is a tree most like a plant, an animal, or a car? …one word…") through `userMessageOnly`. The trimmed answers are counted in a `HashMap`, and the answer with the highest count is logged as the "most common answer".

This step is not connected to the code review. It shows the idea of detecting hallucinations by majority vote, and a source comment links to Hamming codes. It runs **before** the file argument is checked, so all three models are called on every run, even when the path is invalid. When counts tie, the winner is whichever answer the `HashMap` happens to iterate first.

## 2. Read the target file

The path is `args.getNonOptionArgs().get(0)`. If it is not a regular file, the run logs `Not a file` and returns. Otherwise the file's lines are joined with `\n`.

## 3. Reviewer rewrites

Each of `qwen2.5-coder`, `falcon3` and `gemma2` gets a request through `systemUserMessage` with:

- **System prompt:** "You are a helpful code reviewer looking for bugs, security problems, and performance problems in source code"
- **User prompt:** "Please rewrite this class to fix bugs, security problems, and performance problems in this code. If there are no problems then just reply with 'no problem'. " followed by the file contents.

The integration tests reuse the same prompt strings. See [Testing Strategy](../testing/test-strategy.md).

## 4. Filter non-answers

A response is kept in the `responses` map (model → text) only if it is non-null, **longer than 20 characters**, and does **not** start with `"No problem"`. The prefix check is case-sensitive, and the prompt asks for a lowercase `'no problem'`. A bare "no problem" reply is still dropped because it fails the length check. A longer reply that starts with lowercase "no problem…" is kept.

## 5. Choose the result

| Responses kept | Outcome |
|---|---|
| 0 | Logs `<file> no coding recommendations` |
| 1 | Logs `<file> Only response was <text>`. No judge call. |
| ≥2 | Runs the LLM-as-judge step described below |

**Judge step.** The code builds one prompt that lists each kept response as `Choice name is <model> text is ''' <response> '''` and sends it to `gemma2` with the reviewer system prompt. The prompt asks for "only the choice name". The trimmed reply is logged as `AggResponse`.

**Mapping the judge's answer back to a response:**

1. If the reply is exactly a model name, that model's response is the best one.
2. Otherwise, which is the usual case, because judges tend to answer with prose such as "The best choice is **qwen2.5-coder**…" (see `BookStoreProduct.output`), the code searches the reply for each model name. The model whose name appears **earliest** wins.
3. If no model name appears anywhere in the reply, the code picks the first key of the `HashMap`, which is effectively arbitrary.

Either way, the code logs `<file> Best response was <text>`.

## Known limitations

- The earliest-mention heuristic can pick the wrong model when the judge names a losing model first, for example "Unlike falcon3, qwen2.5-coder…".
- `gemma2` both reviews and judges, so it may favour its own answer.
- `HashMap` iteration order controls the order of choices in the judge prompt and the tie-break, so results can change between runs even at temperature 0.
- Output goes only to the log. Nothing is written to a file and the exit code does not report the outcome.
- The pipeline covers a single file. Ideas for extending it are on the [Ollama LLM Client and Helpers](../integrations/ollama-llm-client.md) page.

Related: [Architecture Overview](../architecture/overview.md).
