package com.newsaggregator.api.service;

import com.newsaggregator.api.model.NewsApiResponse;
import com.newsaggregator.api.model.NewsArticle;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private final WebClient newsApiClient;
    // Add other WebClients for different APIs

    public NewsService(WebClient.Builder webClientBuilder) {
        this.newsApiClient = webClientBuilder.baseUrl("https://newsapi.org/v2").build();
    }

    public List<NewsArticle> fetchNews(Set<String> preferences) {
        List<Mono<NewsApiResponse>> apiCalls = preferences.stream()
                .map(preference -> newsApiClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/top-headlines")
                                .queryParam("q", preference)
                                .queryParam("apiKey", "YOUR_NEWSAPI_KEY") // Replace with your key
                                .build())
                        .retrieve()
                        .bodyToMono(NewsApiResponse.class))
                .collect(Collectors.toList());

        return Mono.zip(apiCalls, results -> {
            List<NewsArticle> combinedArticles = new ArrayList<>();
            for (Object result : results) {
                if (result instanceof NewsApiResponse) {
                    combinedArticles.addAll(((NewsApiResponse) result).getArticles());
                }
            }
            return combinedArticles;
        }).block();
    }
}