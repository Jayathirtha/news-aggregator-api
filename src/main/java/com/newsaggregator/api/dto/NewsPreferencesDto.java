package com.newsaggregator.api.dto;


import java.util.HashSet;
import java.util.Set;

public class NewsPreferencesDto {
    private Set<String> categories = new HashSet<>();
    private Set<String> sources = new HashSet<>();

    // Getters and setters
    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories; }
    public Set<String> getSources() { return sources; }
    public void setSources(Set<String> sources) { this.sources = sources; }
}
