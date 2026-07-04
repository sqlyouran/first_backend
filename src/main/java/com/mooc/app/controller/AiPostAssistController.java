package com.mooc.app.controller;

import com.mooc.app.dto.AiPostAssistRequest;
import com.mooc.app.dto.response.AiPostAssistResponse;
import com.mooc.app.service.AiPostAssistService;
import com.mooc.app.service.JwtService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiPostAssistController {

    private final AiPostAssistService aiPostAssistService;
    private final JwtService jwtService;

    public AiPostAssistController(AiPostAssistService aiPostAssistService, JwtService jwtService) {
        this.aiPostAssistService = aiPostAssistService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/ai/post-assist")
    public ResponseEntity<AiPostAssistResponse> assist(
            @Valid @RequestBody AiPostAssistRequest request,
            HttpServletRequest httpRequest) {
        AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        AiPostAssistResponse response = aiPostAssistService.assist(request, requestId);
        return ResponseEntity.ok(response);
    }
}
