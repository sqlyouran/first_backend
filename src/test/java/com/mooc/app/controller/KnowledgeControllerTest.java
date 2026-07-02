package com.mooc.app.controller;

import com.mooc.app.service.KnowledgeBuilderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private KnowledgeBuilderService knowledgeBuilderService;

    @Test
    void rebuild_returns200WithRebuildStartedStatus() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rebuild_started"))
                .andExpect(jsonPath("$.request_id").exists());
    }

    @Test
    void rebuild_triggersAsyncRebuild() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild"));

        verify(knowledgeBuilderService).rebuildAllAsync();
    }
}
