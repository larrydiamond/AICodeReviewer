package com.ldiamond.dli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import io.github.sashirestela.openai.OpenAI;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartText;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

class AppChatWrappersTest {

    private static final String MODEL = "test-model";
    private static final String REPLY = "model reply";

    private final App app = new App();

    private SimpleOpenAI openAI;
    private OpenAI.ChatCompletions chatCompletions;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        openAI = mock(SimpleOpenAI.class);
        chatCompletions = mock(OpenAI.ChatCompletions.class);
        when(openAI.chatCompletions()).thenReturn(chatCompletions);
    }

    private void replyWith(final String content) {
        Chat chat = mock(Chat.class);
        when(chat.firstContent()).thenReturn(content);
        when(chatCompletions.create(any(ChatRequest.class))).thenReturn(CompletableFuture.completedFuture(chat));
    }

    private void failWith(final RuntimeException e) {
        when(chatCompletions.create(any(ChatRequest.class))).thenReturn(CompletableFuture.failedFuture(e));
    }

    private ChatRequest capturedRequest() {
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatCompletions).create(captor.capture());
        return captor.getValue();
    }

    private Path imageFile() throws IOException {
        return Files.write(tempDir.resolve("pixel.png"), new byte[] { 1, 2, 3, 4 });
    }

    private static List<?> userContentParts(final ChatMessage message) {
        assertThat(message).isInstanceOf(UserMessage.class);
        Object content = ((UserMessage) message).getContent();
        assertThat(content).isInstanceOf(List.class);
        return (List<?>) content;
    }

    private static Object textOf(final Object part) {
        assertThat(part).isInstanceOf(ContentPartText.class);
        return ((ContentPartText) part).getText();
    }

    private static String imageUrlOf(final Object part) {
        assertThat(part).isInstanceOf(ContentPartImageUrl.class);
        return ((ContentPartImageUrl) part).getImageUrl().getUrl();
    }

    private static void assertDeterministic(final ChatRequest request) {
        assertThat(request.getModel()).isEqualTo(MODEL);
        assertThat(request.getTemperature()).isEqualTo(0.0);
    }

    @Nested
    class UserMessageOnly {

        @Test
        void returnsFirstContent() {
            replyWith(REPLY);

            assertThat(app.userMessageOnly(openAI, MODEL, "hello")).isEqualTo(REPLY);
        }

        @Test
        void sendsSingleUserMessageWithZeroTemperature() {
            replyWith(REPLY);

            app.userMessageOnly(openAI, MODEL, "hello");

            ChatRequest request = capturedRequest();
            assertDeterministic(request);
            assertThat(request.getMessages()).hasSize(1);
            assertThat(request.getMessages().get(0)).isInstanceOf(UserMessage.class);
            assertThat(((UserMessage) request.getMessages().get(0)).getContent()).isEqualTo("hello");
        }

        @Test
        void propagatesFailure() {
            failWith(new IllegalStateException("boom"));

            assertThatThrownBy(() -> app.userMessageOnly(openAI, MODEL, "hello"))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class SystemUserMessage {

        @Test
        void returnsFirstContent() {
            replyWith(REPLY);

            assertThat(app.systemUserMessage(openAI, MODEL, "sys", "user")).isEqualTo(REPLY);
        }

        @Test
        void sendsSystemMessageBeforeUserMessage() {
            replyWith(REPLY);

            app.systemUserMessage(openAI, MODEL, "sys", "user");

            ChatRequest request = capturedRequest();
            assertDeterministic(request);
            assertThat(request.getMessages()).hasSize(2);
            assertThat(request.getMessages().get(0)).isInstanceOf(SystemMessage.class);
            assertThat(((SystemMessage) request.getMessages().get(0)).getContent()).isEqualTo("sys");
            assertThat(request.getMessages().get(1)).isInstanceOf(UserMessage.class);
            assertThat(((UserMessage) request.getMessages().get(1)).getContent()).isEqualTo("user");
        }

        @Test
        void propagatesFailure() {
            failWith(new IllegalStateException("boom"));

            assertThatThrownBy(() -> app.systemUserMessage(openAI, MODEL, "sys", "user"))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class UserTextAttachment {

        @Test
        void returnsFirstContent() {
            replyWith(REPLY);

            assertThat(app.userTextAttachment(openAI, MODEL, "sys", "user", "attachment")).isEqualTo(REPLY);
        }

        @Test
        void sendsAttachmentAsSeparateContentPart() {
            replyWith(REPLY);

            app.userTextAttachment(openAI, MODEL, "sys", "user", "attachment");

            ChatRequest request = capturedRequest();
            assertDeterministic(request);
            assertThat(request.getMessages()).hasSize(2);
            assertThat(((SystemMessage) request.getMessages().get(0)).getContent()).isEqualTo("sys");
            List<?> parts = userContentParts(request.getMessages().get(1));
            assertThat(parts).hasSize(2);
            assertThat(textOf(parts.get(0))).isEqualTo("user");
            assertThat(textOf(parts.get(1))).isEqualTo("attachment");
        }

        @Test
        void returnsEmptyStringOnFailure() {
            failWith(new IllegalStateException("boom"));

            assertThat(app.userTextAttachment(openAI, MODEL, "sys", "user", "attachment")).isEmpty();
        }
    }

    @Nested
    class UserImage {

        @Test
        void returnsFirstContent() throws IOException {
            replyWith(REPLY);

            assertThat(app.userImage(openAI, MODEL, "describe", imageFile().toString())).isEqualTo(REPLY);
        }

        @Test
        void sendsTextAndBase64ImageParts() throws IOException {
            replyWith(REPLY);

            app.userImage(openAI, MODEL, "describe", imageFile().toString());

            ChatRequest request = capturedRequest();
            assertDeterministic(request);
            assertThat(request.getMessages()).hasSize(1);
            List<?> parts = userContentParts(request.getMessages().get(0));
            assertThat(parts).hasSize(2);
            assertThat(textOf(parts.get(0))).isEqualTo("describe");
            assertThat(imageUrlOf(parts.get(1))).isEqualTo("data:image/png;base64,AQIDBA==");
        }

        @Test
        void returnsEmptyStringWhenImageMissing() {
            assertThat(app.userImage(openAI, MODEL, "describe", tempDir.resolve("missing.png").toString())).isEmpty();
            verify(chatCompletions, never()).create(any(ChatRequest.class));
        }

        @Test
        void returnsEmptyStringOnFailure() throws IOException {
            failWith(new IllegalStateException("boom"));

            assertThat(app.userImage(openAI, MODEL, "describe", imageFile().toString())).isEmpty();
        }
    }

    @Nested
    class UserSystemImage {

        @Test
        void returnsFirstContent() throws IOException {
            replyWith(REPLY);

            assertThat(app.userSystemImage(openAI, MODEL, "describe", "sys", imageFile().toString())).isEqualTo(REPLY);
        }

        @Test
        void sendsSystemMessageThenTextAndImageParts() throws IOException {
            replyWith(REPLY);

            app.userSystemImage(openAI, MODEL, "describe", "sys", imageFile().toString());

            ChatRequest request = capturedRequest();
            assertDeterministic(request);
            assertThat(request.getMessages()).hasSize(2);
            assertThat(request.getMessages().get(0)).isInstanceOf(SystemMessage.class);
            assertThat(((SystemMessage) request.getMessages().get(0)).getContent()).isEqualTo("sys");
            List<?> parts = userContentParts(request.getMessages().get(1));
            assertThat(parts).hasSize(2);
            assertThat(textOf(parts.get(0))).isEqualTo("describe");
            assertThat(imageUrlOf(parts.get(1))).startsWith("data:image/png;base64,");
        }

        @Test
        void returnsEmptyStringWhenImageMissing() {
            assertThat(app.userSystemImage(openAI, MODEL, "describe", "sys", tempDir.resolve("missing.png").toString()))
                .isEmpty();
            verify(chatCompletions, never()).create(any(ChatRequest.class));
        }

        @Test
        void returnsEmptyStringOnFailure() throws IOException {
            failWith(new IllegalStateException("boom"));

            assertThat(app.userSystemImage(openAI, MODEL, "describe", "sys", imageFile().toString())).isEmpty();
        }
    }
}
