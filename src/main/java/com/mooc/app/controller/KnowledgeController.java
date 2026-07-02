package com.mooc.app.controller;

import com.mooc.app.dto.response.KnowledgeRebuildResponse;
import com.mooc.app.service.KnowledgeBuilderService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeController {

    private final KnowledgeBuilderService knowledgeBuilderService;

    public KnowledgeController(KnowledgeBuilderService knowledgeBuilderService) {
        this.knowledgeBuilderService = knowledgeBuilderService;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<KnowledgeRebuildResponse> rebuild(HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        knowledgeBuilderService.rebuildAllAsync();
        return ResponseEntity.ok(new KnowledgeRebuildResponse(requestId, "rebuild_started"));
    }
}
