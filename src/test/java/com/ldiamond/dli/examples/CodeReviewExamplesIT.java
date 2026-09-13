package com.ldiamond.dli.examples;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

// Needs a real Ollama server at localhost:11434; run via `./gradlew integrationTest`, excluded from `test`.
// Calls Ollama's /v1/chat/completions directly rather than through simple-openai: that client never
// deserializes Ollama's "reasoning" field (it expects "reasoningContent"), so a reasoning model's analysis
// is lost when maxTokens is hit before the final "content" is written.
@Tag("integration")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CodeReviewExamplesIT {

    private static final String MODEL = "ornith-1.5:9b";
    private static final int MAX_RESPONSE_TOKENS = 2000;
    private static final String CODE_REVIEW_SYSTEM_MESSAGE =
        "You are a helpful code reviewer looking for bugs, security problems, and performance problems in source code";
    private static final String CODE_REVIEW_USER_MESSAGE_PREFIX =
        "Please rewrite this class to fix bugs, security problems, and performance problems in this code.   If there are no problems then just reply with 'no problem'. ";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bookStoreProduct_flagsSwappedConstructorAssignment() throws IOException, InterruptedException {
        String response = review("BookStoreProduct.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("constructor");
    }

    @Test
    void circumference_flagsWrongPiConstant() throws IOException, InterruptedException {
        String response = review("Circumference.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("pi");
    }

    @Test
    void inefficientStringConcatenation_recommendsStringBuilder() throws IOException, InterruptedException {
        String response = review("Inefficientstringconcatenation.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("stringbuilder");
    }

    @Test
    void petStoreProduct_flagsConstructorBug() throws IOException, InterruptedException {
        String response = review("PetStoreProduct.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("constructor");
    }

    @Test
    void previousReference_flagsIndexBug() throws IOException, InterruptedException {
        String response = review("Previousreference.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("index");
    }

    @Test
    void usedTheWrongCollection_recommendsHashMap() throws IOException, InterruptedException {
        String response = review("Usedthewrongcollection.java");
        assertThat(response.toLowerCase()).doesNotStartWith("no problem");
        assertThat(response.toLowerCase()).contains("hashmap");
    }

    private String review(final String fixtureFileName) throws IOException, InterruptedException {
        String contents = readFixture(fixtureFileName);
        Map<String, Object> requestBody = Map.of(
            "model", MODEL,
            "temperature", 0.0,
            "max_tokens", MAX_RESPONSE_TOKENS,
            "messages", List.of(
                Map.of("role", "system", "content", CODE_REVIEW_SYSTEM_MESSAGE),
                Map.of("role", "user", "content", CODE_REVIEW_USER_MESSAGE_PREFIX + contents)));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode message = objectMapper.readTree(response.body()).at("/choices/0/message");
        return message.path("reasoning").asText("") + " " + message.path("content").asText("");
    }

    private String readFixture(final String fixtureFileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/examples/" + fixtureFileName)) {
            if (in == null) {
                throw new IOException("Fixture not found on classpath: examples/" + fixtureFileName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
