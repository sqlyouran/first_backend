package com.mooc.app.service;

import com.mooc.app.dto.AiPostAssistRequest;
import com.mooc.app.dto.response.AiPostAssistResponse;
import com.mooc.app.exception.AiPostAssistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPostAssistServiceTest {

    @Mock private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;

    private AiPostAssistService service;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        service = new AiPostAssistService(chatClient);
    }

    private void stubChatClientChain(String responseContent) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }

    @Test
    void generateTitle_success() {
        stubChatClientChain("Exploring the Ancient Streets of Hangzhou");
        AiPostAssistRequest request = new AiPostAssistRequest("generate_title", "Some content about Hangzhou", null);

        AiPostAssistResponse response = service.assist(request, "req-1");

        assertEquals("req-1", response.getRequestId());
        assertEquals("Exploring the Ancient Streets of Hangzhou", response.getResult());
    }

    @Test
    void generateTitle_truncatesLongTitle() {
        String longTitle = "A".repeat(250);
        stubChatClientChain(longTitle);
        AiPostAssistRequest request = new AiPostAssistRequest("generate_title", "content", null);

        AiPostAssistResponse response = service.assist(request, "req-2");

        assertEquals(200, response.getResult().length());
    }

    @Test
    void recommendTags_success() {
        stubChatClientChain("[\"culture\",\"food\",\"history\"]");
        AiPostAssistRequest request = new AiPostAssistRequest("recommend_tags", "A post about Beijing food", "Beijing Food Guide");

        AiPostAssistResponse response = service.assist(request, "req-3");

        assertEquals("[\"culture\",\"food\",\"history\"]", response.getResult());
    }

    @Test
    void recommendTags_stripsMarkdownFences() {
        stubChatClientChain("```json\n[\"travel\",\"nature\"]\n```");
        AiPostAssistRequest request = new AiPostAssistRequest("recommend_tags", "content", null);

        AiPostAssistResponse response = service.assist(request, "req-4");

        assertEquals("[\"travel\",\"nature\"]", response.getResult());
    }

    @Test
    void polish_success() {
        String polished = "# Improved Title\n\nThis is polished content.";
        stubChatClientChain(polished);
        AiPostAssistRequest request = new AiPostAssistRequest("polish", "# Original\n\nRough content.", null);

        AiPostAssistResponse response = service.assist(request, "req-5");

        assertEquals(polished, response.getResult());
    }

    @Test
    void generateTitle_emptyResponse_throws() {
        stubChatClientChain("");
        AiPostAssistRequest request = new AiPostAssistRequest("generate_title", "content", null);

        AiPostAssistException ex = assertThrows(AiPostAssistException.class, () -> service.assist(request, "req-6"));
        assertEquals("ai_generation_failed", ex.getErrorCode());
    }

    @Test
    void nullResponse_throws() {
        stubChatClientChain(null);
        AiPostAssistRequest request = new AiPostAssistRequest("polish", "content", null);

        AiPostAssistException ex = assertThrows(AiPostAssistException.class, () -> service.assist(request, "req-7"));
        assertEquals("ai_generation_failed", ex.getErrorCode());
    }
}
