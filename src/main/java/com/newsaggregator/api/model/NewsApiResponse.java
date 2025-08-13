package com.newsaggregator.api.model;

import java.util.List;

public class NewsApiResponse {
    private List<NewsArticle> articles;

    // Getters and setters
    public List<NewsArticle> getArticles() { return articles; }
    public void setArticles(List<NewsArticle> articles) { this.articles = articles; }
}
