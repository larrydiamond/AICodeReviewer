package com.ldiamond.dli;

import java.io.IOException;
import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.SimpleOpenAI.SimpleOpenAIBuilder;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl.ImageUrl;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartText;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import lombok.extern.java.Log;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.support.Base64Util;
import io.github.sashirestela.openai.support.Base64Util.MediaType;

@Log
@Component
public class App implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("ApplicationRunner: " + args.getOptionNames());

        Config config = null;
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			config = objectMapper.readValue(new File(args.getNonOptionArgs().get(0)), Config.class);
		} catch (IOException e) {
			e.printStackTrace();
		}	

		if (config == null) {
			System.out.println ("Configuration required");
            return;
		}

        Map<String,SimpleOpenAI> servers = new HashMap<>();
        for (Server openaiServer : config.getServers()) {
            SimpleOpenAIBuilder builder = SimpleOpenAI.builder();
            if (openaiServer.getApiKey() != null)
                builder.apiKey(openaiServer.getApiKey());

            if (openaiServer.getOrganizationId() != null)
                builder.organizationId(openaiServer.getOrganizationId());

            if (openaiServer.getProjectId() != null)
                builder.projectId(openaiServer.getProjectId());

            if (openaiServer.getBaseUrl() != null)
                builder.baseUrl(openaiServer.getBaseUrl());

            SimpleOpenAI openAI = builder.build();
            servers.put(openaiServer.getName(), openAI);
        }
        
        Map<String,Model> models = new HashMap<>();
        for (Model modelConfiguration : config.getModels()) {
            models.put(modelConfiguration.getModelName(), modelConfiguration);
        }

        Path filePath = Paths.get(args.getNonOptionArgs().get(1));
        if (Files.isRegularFile(filePath)) {
            List<String> thisFile = Files.readAllLines(filePath);
            String contents = String.join("\n", thisFile);

            String codeReviewSystemMessage = "You are a helpful code reviewer looking for bugs, security problems, and performance problems in source code";
            String codeReviewUserMessagePrefix = "Please rewrite this class to fix bugs, security problems, and performance problems in this code.   If there are no problems then just reply with 'no problem'. ";

            Map<String,String> responses = new HashMap<>();
            for (Map.Entry<String,Model> modelEntry : models.entrySet()) {
                String model = modelEntry.getKey();
                SimpleOpenAI openAI = servers.get(modelEntry.getValue().getServerName());
                String response = systemUserMessage(openAI, model, codeReviewSystemMessage, codeReviewUserMessagePrefix + contents);
                if ((response != null) && (response.length() > 20) && (!response.startsWith("No problem"))) {
                    responses.put(model, response);
//                    log.info(model + " " + response);
                }
            }

            if (responses.size() > 1) {
                // LLM as a judge https://huggingface.co/learn/cookbook/en/llm_judge
                StringBuilder sb = new StringBuilder();
                sb.append("Different coders rewrote the class below to remove bugs, security problems, and performance problems.  Please return only the choice name of the choice that performed best: ");
                for (Map.Entry<String,String> entry : responses.entrySet()) {
                    sb.append(" Choice name is " + entry.getKey() + " text is ''' " + entry.getValue() + " ''' , ");
                }

                SimpleOpenAI openAI = servers.get(models.get(config.getJudgemodel()).getServerName());
                String aggResponse = systemUserMessage(openAI, config.getJudgemodel(), codeReviewSystemMessage, sb.toString()).trim();
                log.info(filePath.toString() + " AggResponse = " + aggResponse);

                String bestResponse = responses.get(aggResponse);
                if (bestResponse != null) {
                    log.info(filePath.toString() + " Best response was " + bestResponse);
                } else {
                    int earliest = Integer.MAX_VALUE;
                    String earliestName = responses.keySet().iterator().next();
                    for (Map.Entry<String,String> entry : responses.entrySet()) {
                        int responseOffset = aggResponse.indexOf(entry.getKey());
                        if (responseOffset != -1) {
                            if (responseOffset < earliest) {
                                earliest = responseOffset;
                                earliestName = entry.getKey();
                            }
                        }
                    }
                    log.info(filePath.toString() + " Best response was " + responses.get(earliestName));
                }
            } else {
                if (responses.size() == 1) {
                    log.info(filePath.toString() + " Only response was " + responses.values().iterator().next());
                } else {
                    log.info (filePath.toString() + " no coding recommendations");
                }
            }

        } else {
            log.info("Not a file");
        }
        
    }

    public String userMessageOnly(final SimpleOpenAI openAI, final String model, final String userMessage) {
//        final long startTime = System.currentTimeMillis();
        final ChatRequest chatRequest = ChatRequest.builder()
            .model(model)
            .message(UserMessage.of(userMessage))
            .temperature(0.0)
            .maxCompletionTokens(-1)
            .build();
        final CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
        final Chat chatResponse = futureChat.join();
//        final long stopTime = System.currentTimeMillis();
//        log.info(model + " userMessageOnly took " +  (stopTime - startTime) + " ms");
        return chatResponse.firstContent();
    }

    public String systemUserMessage (final SimpleOpenAI openAI, final String model, final String systemMessage, final String userMessage) {
//        final long startTime = System.currentTimeMillis();
        final ChatRequest chatRequest = ChatRequest.builder()
            .model(model)
            .message(SystemMessage.of(systemMessage))
            .message(UserMessage.of(userMessage))
            .temperature(0.0)
            .maxCompletionTokens(-1)
            .build();
        final CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
        final Chat chatResponse = futureChat.join();
//        final long stopTime = System.currentTimeMillis();
//        log.info(model + " userMessageOnly took " +  (stopTime - startTime) + " ms");
        return chatResponse.firstContent();
    }

    public String userImage (final SimpleOpenAI openAI, final String model, final String userMessage, final String imageFileName) {
        try {
//            final long startTime = System.currentTimeMillis();
            final ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .messages(List.of(
                    UserMessage.of(List.of(
                        ContentPartText.of(
                                userMessage),
                        ContentPartImageUrl.of(ImageUrl.of(Base64Util.encode(imageFileName, MediaType.IMAGE)))))))
                .temperature(0.0)
                .maxCompletionTokens(-1)
                .build();
            final CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
            final Chat chatResponse = futureChat.join();
//            final long stopTime = System.currentTimeMillis();
//            log.info(model + " userImage took " +  (stopTime - startTime) + " ms");
            return chatResponse.firstContent();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Could not userImage for model " + model);
            return "";
        }
    }

    public String userTextAttachment (final SimpleOpenAI openAI, final String model, final String systemMessage, final String userMessage, final String textAttachment) {
        try {
//            final long startTime = System.currentTimeMillis();
            final ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .messages(List.of(
                    SystemMessage.of(systemMessage),
                    UserMessage.of(List.of(
                        ContentPartText.of(userMessage),
                        ContentPartText.of(textAttachment)))))
                .temperature(0.0)
                .maxCompletionTokens(-1)
                .build();
            final CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
            final Chat chatResponse = futureChat.join();
//            final long stopTime = System.currentTimeMillis();
//            log.info(model + " userTextAttachment took " +  (stopTime - startTime) + " ms");
            return chatResponse.firstContent();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Could not userTextAttachment for model " + model);
            return "";
        }
    }

    public String userSystemImage (final SimpleOpenAI openAI, final String model, final String userMessage, final String systemMessage, final String imageFileName) {
        try {
//            final long startTime = System.currentTimeMillis();
            final ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .message(SystemMessage.of(systemMessage))  
                .messages(List.of(
                    UserMessage.of(List.of(
                        ContentPartText.of(
                                userMessage),
                        ContentPartImageUrl.of(ImageUrl.of(Base64Util.encode(imageFileName, MediaType.IMAGE)))))))
                .temperature(0.0)
                .maxCompletionTokens(-1)
                .build();
            final CompletableFuture<Chat> futureChat = openAI.chatCompletions().create(chatRequest);
            final Chat chatResponse = futureChat.join();
//            final long stopTime = System.currentTimeMillis();
//            log.info(model + " userSystemImage took " +  (stopTime - startTime) + " ms");
            return chatResponse.firstContent();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Could not userSystemImage for model " + model);
            return "";
        }
    }

    public String getTextFromPDF (final String filename) {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(filename))) {
            return pdfTextStripper.getText(document);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getHtmlFromPDF (final String filename) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(filename))) {
            PDFText2HTML pdfTextStripper = new PDFText2HTML();
            return pdfTextStripper.getText(document);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String readDirectoryTreeToString(final String directoryPath, final String extension) throws IOException {
        final List<String> fileContents = new ArrayList<>();
        final Path directory = Paths.get(directoryPath);
//        log.info ("Directory = " + directory.toString());

        Queue<Path> dirs = new LinkedList<>();
        dirs.add(directory);

        while (!dirs.isEmpty()) {
            Path dir = dirs.poll();
            if (dir != null) {
                DirectoryStream<Path> dsp = Files.newDirectoryStream(dir);
                for (Path p : dsp) {
                    if (Files.isDirectory(p)) {
                        dirs.add(p);
                    } else {
                        if (p.toString().endsWith(".java")) {
                            List<String> thisFile = Files.readAllLines(p);
//                            log.info("file contents for " + p.toString() + " = " + thisFile.toString());
                            fileContents.addAll(thisFile);
                        }                        
                    }
                }
                dsp.close();
            }
        }

        return String.join("", fileContents);
    }
    
}
    
