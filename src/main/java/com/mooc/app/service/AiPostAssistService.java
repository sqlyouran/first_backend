package com.mooc.app.service;

import com.mooc.app.dto.AiPostAssistRequest;
import com.mooc.app.dto.response.AiPostAssistResponse;
import com.mooc.app.exception.AiPostAssistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiPostAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiPostAssistService.class);

    private static final String GENERATE_TITLE_SYSTEM_PROMPT = """
            You are a travel content editor. Generate a compelling, SEO-friendly title \
            (max 200 characters) for a travel blog post based on its content. \
            Return ONLY the title, no quotes, no extra explanation.""";

    private static final String RECOMMEND_TAGS_SYSTEM_PROMPT = """
            You are a travel content editor. Recommend 3-8 relevant tags for a travel blog post. \
            Return ONLY a JSON array of strings, e.g. ["culture","food","history"]. \
            No extra explanation, no markdown fences.""";

    private static final String POLISH_SYSTEM_PROMPT = """
            You are a professional travel editor. Rewrite the following Markdown content to \
            improve readability, grammar, and engagement. Keep the Markdown formatting, \
            preserve core information, and use vivid but concise language. \
            Return ONLY the polished Markdown, no extra explanation.""";

    private static final int MAX_TITLE_LENGTH = 200;

    private final ChatClient chatClient;

    public AiPostAssistService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AiPostAssistResponse assist(AiPostAssistRequest request, String requestId) {
        String result = switch (request.action()) {
            case "generate_title" -> generateTitle(request.content());
            case "recommend_tags" -> recommendTags(request.content(), request.title());
            case "polish" -> polish(request.content());
            default -> throw new AiPostAssistException(HttpStatus.BAD_REQUEST,
                    "invalid_action", "Unsupported action: " + request.action());
        };
        return new AiPostAssistResponse(requestId, result);
    }

    String generateTitle(String content) {
        log.debug("Generating title from content ({} chars)", content.length());
        String result = chatClient.prompt()
                .system(GENERATE_TITLE_SYSTEM_PROMPT)
                .user(content)
                .call()
                .content();
        if (result == null || result.isBlank()) {
            throw new AiPostAssistException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ai_generation_failed", "AI returned empty title");
        }
        result = result.trim();
        if (result.length() > MAX_TITLE_LENGTH) {
            result = result.substring(0, MAX_TITLE_LENGTH);
        }
        return result;
    }

    String recommendTags(String content, String title) {
        log.debug("Recommending tags from content ({} chars)", content.length());
        String userPrompt = title != null && !title.isBlank()
                ? "Title: " + title + "\n\nContent:\n" + content
                : content;
        String result = chatClient.prompt()
                .system(RECOMMEND_TAGS_SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
        if (result == null || result.isBlank()) {
            throw new AiPostAssistException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ai_generation_failed", "AI returned empty tags");
        }
        // Strip markdown fences if present
        result = result.trim();
        if (result.startsWith("```")) {
            result = result.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        return result;
    }

    String polish(String content) {
        log.debug("Polishing content ({} chars)", content.length());
        String result = chatClient.prompt()
                .system(POLISH_SYSTEM_PROMPT)
                .user(content)
                .call()
                .content();
        if (result == null || result.isBlank()) {
            throw new AiPostAssistException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ai_generation_failed", "AI returned empty polished content");
        }
        return result;
    }
}
