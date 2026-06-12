package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationCodeStore codeStore;

    private String authorToken;
    private String otherToken;

    @BeforeEach
    void setup() throws Exception {
        authorToken = registerAndLogin("author@example.com", "Password1");
        otherToken = registerAndLogin("other@example.com", "Password1");
    }

    // === 6.3 POST /api/posts ===

    @Test
    void createPost_validRequest_returns201() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "My First Post", "# Hello\nWorld", "https://example.com/img.jpg",
                List.of("travel", "food"), null);

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("My First Post"))
            .andExpect(jsonPath("$.status").value("published"))
            .andExpect(jsonPath("$.created_at").isNotEmpty());
    }

    @Test
    void createPost_draftStatus_returns201WithDraft() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "Draft Post", "# Draft", null, List.of(), "draft");

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    void createPost_noToken_returns401() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "Title", "Content", null, List.of(), null);

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_emptyTitle_returns422() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "", "Content", null, List.of(), null);

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void createPost_emptyContent_returns422() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "Title", "", null, List.of(), null);

        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    // === 6.4 PUT /api/posts/{id} ===

    @Test
    void updatePost_validRequest_returns200() throws Exception {
        String postId = createPost("Original Title");
        UpdatePostRequest req = new UpdatePostRequest("Updated Title", null, null, null, null);

        mockMvc.perform(put("/api/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.content").value("Content body"));
    }

    @Test
    void updatePost_notAuthor_returns403() throws Exception {
        String postId = createPost("My Post");
        UpdatePostRequest req = new UpdatePostRequest("Hacked", null, null, null, null);

        mockMvc.perform(put("/api/posts/" + postId)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void updatePost_notFound_returns404() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("Title", null, null, null, null);

        mockMvc.perform(put("/api/posts/00000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void updatePost_noToken_returns401() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("Title", null, null, null, null);

        mockMvc.perform(put("/api/posts/00000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    // === 6.5 DELETE /api/posts/{id} ===

    @Test
    void deletePost_validRequest_returns204() throws Exception {
        String postId = createPost("To Delete");

        mockMvc.perform(delete("/api/posts/" + postId)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isNoContent());

        // Verify no longer accessible
        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletePost_notAuthor_returns403() throws Exception {
        String postId = createPost("Not Mine");

        mockMvc.perform(delete("/api/posts/" + postId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void deletePost_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/posts/00000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void deletePost_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/posts/00000000-0000-0000-0000-000000000001"))
            .andExpect(status().isUnauthorized());
    }

    // === 6.6 GET /api/posts/{id} ===

    @Test
    void getPost_published_returns200() throws Exception {
        String postId = createPost("Public Post");

        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(postId))
            .andExpect(jsonPath("$.title").value("Public Post"))
            .andExpect(jsonPath("$.content").value("Content body"))
            .andExpect(jsonPath("$.status").value("published"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void getPost_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/00000000-0000-0000-0000-000000000001"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void getPost_draft_returns404() throws Exception {
        CreatePostRequest req = new CreatePostRequest("Draft", "Content", null, List.of(), "draft");
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        String postId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getPost_archived_returns404() throws Exception {
        CreatePostRequest req = new CreatePostRequest("Archived", "Content", null, List.of(), "archived");
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        String postId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/posts/" + postId))
            .andExpect(status().isNotFound());
    }

    // === 6.7 GET /api/posts ===

    @Test
    void listPosts_defaultPage_returns200() throws Exception {
        createPost("Post 1");
        createPost("Post 2");

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[0].content").doesNotExist());
    }

    @Test
    void listPosts_customPagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            createPost("Post " + i);
        }

        mockMvc.perform(get("/api/posts?page=1&size=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void listPosts_onlyPublished() throws Exception {
        createPost("Published");
        CreatePostRequest draftReq = new CreatePostRequest("Draft", "Content", null, List.of(), "draft");
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(draftReq)));

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].status").value("published"));
    }

    @Test
    void listPosts_sizeCapAt100() throws Exception {
        mockMvc.perform(get("/api/posts?size=200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }

    // === 6.1 Interaction stats fields ===

    @Test
    void listPosts_defaultPage_includesInteractionFields() throws Exception {
        createPost("Post A");
        createPost("Post B");

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].comment_count").isNumber())
            .andExpect(jsonPath("$.items[0].up_vote_count").isNumber())
            .andExpect(jsonPath("$.items[0].bookmark_count").isNumber())
            .andExpect(jsonPath("$.has_more").isBoolean());
    }

    // === 6.2 Interaction stats correctness ===

    @Test
    void listPosts_withInteractions_correctCounts() throws Exception {
        String postId = createPost("Interactive Post");

        // Add a comment
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Great post!\"}"));

        // Add an upvote
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote_type\": \"up\"}"));

        // Add a bookmark
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + otherToken));

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].comment_count").value(1))
            .andExpect(jsonPath("$.items[0].up_vote_count").value(1))
            .andExpect(jsonPath("$.items[0].bookmark_count").value(1));
    }

    // === 6.3 sort=most_upvoted ===

    @Test
    void listPosts_sortMostUpvoted_correctOrder() throws Exception {
        String post1 = createPost("Low Votes");
        String post2 = createPost("High Votes");

        // post2 gets 2 upvotes, post1 gets 1
        mockMvc.perform(post("/api/posts/" + post2 + "/vote")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote_type\": \"up\"}"));
        mockMvc.perform(post("/api/posts/" + post2 + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote_type\": \"up\"}"));
        mockMvc.perform(post("/api/posts/" + post1 + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote_type\": \"up\"}"));

        mockMvc.perform(get("/api/posts?sort=most_upvoted"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].title").value("High Votes"))
            .andExpect(jsonPath("$.items[0].up_vote_count").value(2))
            .andExpect(jsonPath("$.items[1].title").value("Low Votes"));
    }

    // === 6.4 sort=most_commented ===

    @Test
    void listPosts_sortMostCommented_correctOrder() throws Exception {
        String post1 = createPost("Few Comments");
        String post2 = createPost("Many Comments");

        // post2 gets 2 comments
        mockMvc.perform(post("/api/posts/" + post2 + "/comments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Comment 1\"}"));
        mockMvc.perform(post("/api/posts/" + post2 + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Comment 2\"}"));
        // post1 gets 1 comment
        mockMvc.perform(post("/api/posts/" + post1 + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Comment 3\"}"));

        mockMvc.perform(get("/api/posts?sort=most_commented"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].title").value("Many Comments"))
            .andExpect(jsonPath("$.items[0].comment_count").value(2))
            .andExpect(jsonPath("$.items[1].title").value("Few Comments"));
    }

    // === 6.5 cursor pagination ===

    @Test
    void listPosts_cursorPagination_nextCursorAndHasMore() throws Exception {
        for (int i = 0; i < 5; i++) {
            createPost("Post " + i);
            Thread.sleep(20);
        }

        // First page via cursor (use very early cursor to get all), size=3
        MvcResult page1 = mockMvc.perform(get("/api/posts?size=3&sort=latest&cursor=2099-01-01T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.has_more").value(true))
            .andExpect(jsonPath("$.next_cursor").isNotEmpty())
            .andReturn();

        String nextCursor = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("next_cursor").asText();

        // Second page with cursor — should have remaining 2 items, no more
        mockMvc.perform(get("/api/posts?size=3&cursor=" + nextCursor + "&sort=latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.has_more").value(false));
    }

    // === 6.6 invalid sort ===

    @Test
    void listPosts_invalidSort_returns400() throws Exception {
        mockMvc.perform(get("/api/posts?sort=invalid_sort"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    // === 6.7 cursor works for all sorts ===

    @Test
    void listPosts_firstLoad_returnsNextCursorForAnySort() throws Exception {
        createPost("Post A");
        createPost("Post B");

        // Offset mode (first load) with sort=most_upvoted should return next_cursor
        mockMvc.perform(get("/api/posts?sort=most_upvoted"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.next_cursor").isNotEmpty())
            .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void listPosts_cursorPaginationMostUpvoted_correctPaging() throws Exception {
        String post1 = createPost("LowVotes");
        String post2 = createPost("MidVotes");
        String post3 = createPost("HighVotes");

        // post3: 2 votes (author + other), post2: 1 vote (author), post1: 0 votes
        mockMvc.perform(post("/api/posts/" + post3 + "/vote")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vote_type\": \"up\"}"));
        mockMvc.perform(post("/api/posts/" + post3 + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vote_type\": \"up\"}"));
        mockMvc.perform(post("/api/posts/" + post2 + "/vote")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vote_type\": \"up\"}"));

        // First load: size=2, sort=most_upvoted
        MvcResult page1 = mockMvc.perform(get("/api/posts?size=2&sort=most_upvoted"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].title").value("HighVotes"))
            .andExpect(jsonPath("$.has_more").value(true))
            .andExpect(jsonPath("$.next_cursor").isNotEmpty())
            .andReturn();

        String nextCursor = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("next_cursor").asText();

        // Second load: cursor pagination — remaining 1 item
        mockMvc.perform(get("/api/posts?size=2&sort=most_upvoted&cursor=" + nextCursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].title").value("LowVotes"))
            .andExpect(jsonPath("$.has_more").value(false));
    }

    @Test
    void listPosts_cursorPaginationMostCommented_correctPaging() throws Exception {
        String post1 = createPost("FewComments");
        String post2 = createPost("ManyComments");

        // post2: 2 comments, post1: 1 comment
        mockMvc.perform(post("/api/posts/" + post2 + "/comments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"c1\"}"));
        mockMvc.perform(post("/api/posts/" + post2 + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"c2\"}"));
        mockMvc.perform(post("/api/posts/" + post1 + "/comments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"c3\"}"));

        // First load
        MvcResult page1 = mockMvc.perform(get("/api/posts?size=1&sort=most_commented"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].title").value("ManyComments"))
            .andExpect(jsonPath("$.has_more").value(true))
            .andExpect(jsonPath("$.next_cursor").isNotEmpty())
            .andReturn();

        String nextCursor = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("next_cursor").asText();

        // Second load
        mockMvc.perform(get("/api/posts?size=1&sort=most_commented&cursor=" + nextCursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].title").value("FewComments"))
            .andExpect(jsonPath("$.has_more").value(false));
    }

    // === 6.8 listUserPosts enhanced ===

    @Test
    void listUserPosts_sortAndStatsFields() throws Exception {
        String postId = createPost("User Post With Stats");

        // Add an upvote
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vote_type\": \"up\"}"));

        String authorId = extractUserId(authorToken);

        mockMvc.perform(get("/api/users/" + authorId + "/posts?sort=most_upvoted"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].up_vote_count").value(1))
            .andExpect(jsonPath("$.items[0].comment_count").isNumber())
            .andExpect(jsonPath("$.items[0].bookmark_count").isNumber());
    }

    // === 6.8 GET /api/users/{userId}/posts ===

    @Test
    void listUserPosts_hasData_returns200() throws Exception {
        createPost("User Post 1");
        createPost("User Post 2");

        // Extract author ID from token
        String authorId = extractUserId(authorToken);

        mockMvc.perform(get("/api/users/" + authorId + "/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void listUserPosts_noUser_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000099/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)))
            .andExpect(jsonPath("$.total").value(0));
    }

    // === 6.9 cursor boundary ===

    @Test
    void listPosts_cursorNoMoreData_hasMoreFalse() throws Exception {
        createPost("Only Post");

        // Cursor mode with early cursor gets 1 post, hasMore=false
        mockMvc.perform(get("/api/posts?size=20&sort=latest&cursor=2099-01-01T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.has_more").value(false));
    }

    // === Helper methods ===

    private String registerAndLogin(String email, String password) throws Exception {
        codeStore.save(email.toLowerCase(), "123456", 600);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest(email, "123456", password))));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }

    private String createPost(String title) throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                title, "Content body", null, List.of("tag1"), null);

        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String extractUserId(String token) throws Exception {
        // Decode JWT payload (base64 middle segment)
        String payload = token.split("\\.")[1];
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(payload));
        return objectMapper.readTree(decoded).get("sub").asText();
    }
}
