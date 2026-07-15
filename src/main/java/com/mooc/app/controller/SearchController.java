package com.mooc.app.controller;

import com.mooc.app.dto.response.SearchResponse;
import com.mooc.app.dto.response.SearchSuggestItem;
import com.mooc.app.dto.response.SearchSuggestResponse;
import com.mooc.app.service.HybridSearchService;
import com.mooc.app.service.KeywordResult;
import com.mooc.app.service.KeywordSearchService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final HybridSearchService hybridSearchService;
    private final KeywordSearchService keywordSearchService;

    public SearchController(HybridSearchService hybridSearchService,
                            KeywordSearchService keywordSearchService) {
        this.hybridSearchService = hybridSearchService;
        this.keywordSearchService = keywordSearchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam @NotBlank @Size(max = 200) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SearchResponse response = hybridSearchService.search(q, type, city, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggest")
    public ResponseEntity<SearchSuggestResponse> suggest(
            @RequestParam @NotBlank @Size(max = 200) String q,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        List<KeywordResult> results = keywordSearchService.suggest(q);
        List<SearchSuggestItem> items = results.stream()
                .map(r -> new SearchSuggestItem(r.type(), r.slug(), r.name(), null))
                .toList();
        return ResponseEntity.ok(new SearchSuggestResponse(requestId, items));
    }
}
