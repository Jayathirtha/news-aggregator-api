package com.newsaggregator.api.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "newsPreferences" )
public class NewsPreferences {
    @Id
    @Column(name="pref_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private String pId;
    private Set<String> categories = new HashSet<>();
    private Set<String> sources = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private User user;


    public String getPId() { return pId; }
    public void setPId(String userId) { this.pId = pId; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories; }
    public Set<String> getSources() { return sources; }
    public void setSources(Set<String> sources) { this.sources = sources; }

    @Override
    public String toString() {
        return "NewsPreferences{" +
                "userId='" + pId + '\'' +
                ", categories=" + categories +
                ", sources=" + sources +
                ", user=" + user +
                '}';
    }
}
