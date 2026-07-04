package com.mooc.app.service;

import com.mooc.app.entity.CityEntity;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.repository.CityRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class KeywordSearchService {

    private final SpotRepository spotRepository;
    private final PostRepository postRepository;
    private final CityRepository cityRepository;

    @Value("${app.search.keyword-top-k:10}")
    private int keywordTopK;

    @Value("${app.search.suggest-top-k:5}")
    private int suggestTopK;

    public KeywordSearchService(SpotRepository spotRepository,
                                PostRepository postRepository,
                                CityRepository cityRepository) {
        this.spotRepository = spotRepository;
        this.postRepository = postRepository;
        this.cityRepository = cityRepository;
    }

    public List<KeywordResult> search(String query) {
        return doSearch(query, keywordTopK);
    }

    public List<KeywordResult> suggest(String query) {
        return doSearch(query, suggestTopK);
    }

    private List<KeywordResult> doSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String q = query.toLowerCase().trim();
        List<KeywordResult> results = new ArrayList<>();

        for (SpotEntity spot : spotRepository.searchByKeyword(q)) {
            results.add(new KeywordResult(spot.getId(), spot.getName(), spot.getSlug(), "spot", scoreSpot(spot, q)));
        }

        for (PostEntity post : postRepository.searchByKeyword(q)) {
            results.add(new KeywordResult(post.getId(), post.getTitle(), post.getSlug(), "post", scorePost(post, q)));
        }

        for (CityEntity city : cityRepository.searchByKeyword(q)) {
            results.add(new KeywordResult(city.getId(), city.getName(), city.getSlug(), "city", scoreCity(city, q)));
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(KeywordResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private double scoreSpot(SpotEntity spot, String q) {
        double score = 0;
        if (containsQ(spot.getName(), q) || containsQ(spot.getNameZh(), q)) score += 3;
        if (spot.getTags() != null && spot.getTags().stream().anyMatch(t -> containsQ(t, q))) score += 2;
        if (containsQ(spot.getDescription(), q) || containsQ(spot.getDescriptionZh(), q)) score += 1;
        return score;
    }

    private double scorePost(PostEntity post, String q) {
        double score = 0;
        if (containsQ(post.getTitle(), q)) score += 3;
        if (post.getTags() != null && post.getTags().stream().anyMatch(t -> containsQ(t, q))) score += 2;
        if (containsQ(post.getContent(), q)) score += 1;
        return score;
    }

    private double scoreCity(CityEntity city, String q) {
        double score = 0;
        if (containsQ(city.getName(), q) || containsQ(city.getNameZh(), q)) score += 3;
        if (containsQ(city.getDescription(), q)) score += 1;
        return score;
    }

    private boolean containsQ(String text, String q) {
        return text != null && text.toLowerCase().contains(q);
    }
}
