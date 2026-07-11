package com.mooc.app.service;

import com.mooc.app.entity.CityEntity;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.CityRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KnowledgeBuilderServiceTest {

    @Autowired private KnowledgeBuilderService knowledgeBuilderService;
    @Autowired private CityRepository cityRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private PostRepository postRepository;
    @MockBean private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        seedData();
    }

    @Test
    void buildCityDocuments_returnsAllCities() {
        List<Document> docs = knowledgeBuilderService.buildCityDocuments();

        assertEquals(2, docs.size());

        Document beijing = docs.stream()
                .filter(d -> "beijing".equals(d.getMetadata().get("slug")))
                .findFirst().orElseThrow();
        assertEquals("city", beijing.getMetadata().get("entity_type"));
        assertEquals("Beijing", beijing.getMetadata().get("name"));
        assertEquals("北京", beijing.getMetadata().get("name_zh"));
        assertTrue(beijing.getText().contains("Ancient capital"));
        assertTrue(beijing.getText().contains("Autumn"));
    }

    @Test
    void buildSpotDocuments_onlyPublished() {
        CityEntity city = cityRepository.findAll().get(0);
        SpotEntity draftSpot = new SpotEntity();
        draftSpot.setName("Draft Spot");
        draftSpot.setSlug("draft-spot");
        draftSpot.setCityId(city.getId());
        draftSpot.setCityName("Beijing");
        draftSpot.setStatus(SpotStatus.DRAFT);
        spotRepository.save(draftSpot);

        List<Document> docs = knowledgeBuilderService.buildSpotDocuments();

        assertEquals(2, docs.size());
        assertTrue(docs.stream().noneMatch(d -> "draft-spot".equals(d.getMetadata().get("slug"))));
    }

    @Test
    void buildSpotDocuments_containsMetadata() {
        List<Document> docs = knowledgeBuilderService.buildSpotDocuments();

        Document forbidden = docs.stream()
                .filter(d -> "forbidden-city".equals(d.getMetadata().get("slug")))
                .findFirst().orElseThrow();
        assertEquals("spot", forbidden.getMetadata().get("entity_type"));
        assertEquals("Forbidden City", forbidden.getMetadata().get("name"));
        assertEquals("Beijing", forbidden.getMetadata().get("city_name"));
        assertTrue(forbidden.getText().contains("heritage"));
    }

    @Test
    void buildPostDocuments_onlyPublished() {
        PostEntity draftPost = new PostEntity();
        draftPost.setTitle("Draft Post");
        draftPost.setSlug("draft-post");
        draftPost.setContent("Draft content");
        draftPost.setStatus(PostStatus.DRAFT);
        draftPost.setAuthorId(java.util.UUID.randomUUID());
        postRepository.save(draftPost);

        List<Document> docs = knowledgeBuilderService.buildPostDocuments();

        assertEquals(1, docs.size());
        assertTrue(docs.stream().noneMatch(d -> "draft-post".equals(d.getMetadata().get("slug"))));
    }

    @Test
    void buildPostDocuments_containsMetadata() {
        List<Document> docs = knowledgeBuilderService.buildPostDocuments();

        Document post = docs.stream()
                .filter(d -> "beijing-guide".equals(d.getMetadata().get("slug")))
                .findFirst().orElseThrow();
        assertEquals("post", post.getMetadata().get("entity_type"));
        assertEquals("Beijing Guide", post.getMetadata().get("title"));
        assertTrue(post.getText().contains("travel"));
    }

    @Test
    void rebuildAll_writesToVectorStore() {
        knowledgeBuilderService.rebuildAll();

        // 5 documents (2 cities + 2 spots + 1 post) with batch size 2 = 3 batches
        verify(vectorStore, times(3)).add(anyList());
    }

    @Test
    void buildSpotDocuments_includesPracticalInfo() {
        SpotEntity spot = spotRepository.findBySlugAndDeletedFalse("forbidden-city").orElseThrow();
        spot.setTicketPrice("旺季60元/淡季40元");
        spot.setOpeningHours("08:30-17:00");
        spot.setAddress("北京市东城区景山前街4号");
        spotRepository.save(spot);

        List<Document> docs = knowledgeBuilderService.buildSpotDocuments();

        Document doc = docs.stream()
                .filter(d -> "forbidden-city".equals(d.getMetadata().get("slug")))
                .findFirst().orElseThrow();
        assertTrue(doc.getText().contains("Ticket Price: 旺季60元/淡季40元"));
        assertTrue(doc.getText().contains("Opening Hours: 08:30-17:00"));
        assertTrue(doc.getText().contains("Address: 北京市东城区景山前街4号"));
    }

    @Test
    void buildSpotDocuments_omitsNullPracticalFields() {
        List<Document> docs = knowledgeBuilderService.buildSpotDocuments();

        Document doc = docs.stream()
                .filter(d -> "great-wall".equals(d.getMetadata().get("slug")))
                .findFirst().orElseThrow();
        assertFalse(doc.getText().contains("Ticket Price"));
        assertFalse(doc.getText().contains("Opening Hours"));
        assertFalse(doc.getText().contains("Address"));
    }

    private void seedData() {
        CityEntity beijing = new CityEntity();
        beijing.setName("Beijing");
        beijing.setNameZh("北京");
        beijing.setSlug("beijing");
        beijing.setDescription("Ancient capital with imperial grandeur");
        beijing.setBestSeason("Autumn");
        cityRepository.save(beijing);

        CityEntity shanghai = new CityEntity();
        shanghai.setName("Shanghai");
        shanghai.setNameZh("上海");
        shanghai.setSlug("shanghai");
        shanghai.setDescription("Modern metropolis on the Huangpu");
        shanghai.setBestSeason("Spring");
        cityRepository.save(shanghai);

        SpotEntity forbiddenCity = new SpotEntity();
        forbiddenCity.setName("Forbidden City");
        forbiddenCity.setNameZh("故宫");
        forbiddenCity.setSlug("forbidden-city");
        forbiddenCity.setCityId(beijing.getId());
        forbiddenCity.setCityName("Beijing");
        forbiddenCity.setStatus(SpotStatus.PUBLISHED);
        forbiddenCity.setRating(new BigDecimal("4.8"));
        forbiddenCity.setTags(List.of("heritage", "history"));
        forbiddenCity.setDescription("World's largest palace complex");
        spotRepository.save(forbiddenCity);

        SpotEntity greatWall = new SpotEntity();
        greatWall.setName("Great Wall");
        greatWall.setNameZh("长城");
        greatWall.setSlug("great-wall");
        greatWall.setCityId(beijing.getId());
        greatWall.setCityName("Beijing");
        greatWall.setStatus(SpotStatus.PUBLISHED);
        greatWall.setRating(new BigDecimal("4.9"));
        greatWall.setTags(List.of("heritage", "hiking"));
        greatWall.setDescription("Iconic fortification");
        spotRepository.save(greatWall);

        PostEntity beijingPost = new PostEntity();
        beijingPost.setTitle("Beijing Guide");
        beijingPost.setSlug("beijing-guide");
        beijingPost.setContent("A comprehensive guide to Beijing travel.");
        beijingPost.setStatus(PostStatus.PUBLISHED);
        beijingPost.setTags(List.of("beijing", "travel"));
        beijingPost.setAuthorId(java.util.UUID.randomUUID());
        postRepository.save(beijingPost);
    }
}
