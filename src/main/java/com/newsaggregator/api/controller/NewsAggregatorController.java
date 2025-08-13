package com.newsaggregator.api.controller;

import com.newsaggregator.api.dto.NewsPreferencesDto;
import com.newsaggregator.api.dto.UserLoginDto;
import com.newsaggregator.api.dto.UserRegistrationDto;
import com.newsaggregator.api.exception.ResourceNotFoundException;
import com.newsaggregator.api.model.*;
import com.newsaggregator.api.util.WebClientConfig;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import com.newsaggregator.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.newsaggregator.api.util.JwtUtil;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api")
public class NewsAggregatorController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final WebClientConfig webClientConfig;
    private static final Map<String, String> NEWS_APIS = Map.of(
            "NewsAPI", "https://newsapi.org/docs?category={category}&apiKey=fad871a4927a4eb4b072f5d218103050",
            "GNews API", "https://gnews.io/api/v4/search?q=={category}&apiKey=6b9b018ac4134d80485be7141c454553"
            //"api-three",
    );


    public NewsAggregatorController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil, WebClient webClient, WebClientConfig webClientConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.webClientConfig = webClientConfig;
    }


    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        User user = new User();

        if (userRepository.findByUsername(registrationDto.getUsername()).isPresent()) {
            return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Initialize empty preferences for the new user
        NewsPreferences preferences = new NewsPreferences();
        preferences.setPId(registrationDto.getUsername());
        userRepository.save(user);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User user) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            final String jwt = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(Collections.singletonMap("token", jwt));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

 /**   @PostMapping(value = "/auth/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginDto loginDto) {
        String jwt;
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
            jwt = jwtUtil.generateToken(loginDto.getUsername());
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(Collections.singletonMap("token", jwt), HttpStatus.OK);
    } **/


    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        System.out.println(user);
        return ResponseEntity.ok(user.getPreferences());
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@Valid @RequestBody NewsPreferencesDto request) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        NewsPreferences newsPreference = new NewsPreferences();
        newsPreference.setCategories(request.getCategories());
        System.out.println(request.getCategories().toString());
        newsPreference.setSources(request.getSources());
        System.out.println(newsPreference);
        System.out.println("Before USER : " + user);
        user.setPreferences(newsPreference);
        System.out.println(user);

        userRepository.save(user);

        return ResponseEntity.ok("Preferences updated successfully");
    }


    @GetMapping("/news")
    public Mono<List<NewsResponse>> getNews() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not registered"));

        // Use a default category if none is set
        Set<String> categories = user.getPreferences().getCategories();
        if (categories.isEmpty()) {
            categories = Set.of("technology");
        }

        // Use all available news APIs if no sources are specified
        Set<String> sources = user.getPreferences().getSources();
        if (sources.isEmpty()) {
            sources = NEWS_APIS.keySet();
        }

        List<Mono<List<NewsResponse>>> newsRequests = new ArrayList<>();
        for (String sourceKey : sources) {
            String apiUrl = NEWS_APIS.get(sourceKey);
            if (apiUrl != null) {
                // For each category, create a new API call
                for (String category : categories) {
                    newsRequests.add(
                            webClientConfig.webClient().get()
                                    .uri(apiUrl, uriBuilder -> uriBuilder.build(category))
                                    .retrieve()
                                    .bodyToMono(NewsApiResponse.class) // Assumes a common NewsApiResponse structure
                                    .flatMapMany(response -> Flux.fromIterable(response.getArticles()))
                                    .map(article -> {
                                        NewsResponse news = new NewsResponse();
                                        news.setSource(sourceKey);
                                        news.setTitle(article.getTitle());
                                        news.setDescription(article.getDescription());
                                        news.setUrl(article.getUrl());
                                        return news;
                                    })
                                    .collectList()
                                    .onErrorResume(e -> {
                                        // Gracefully handle errors for a single source without failing the entire request
                                        System.err.println("Error fetching news from " + sourceKey + ": " + e.getMessage());
                                        return Mono.just(Collections.emptyList());
                                    })
                    );
                }
            }
        }
        // Combine all Mono<List<NewsResponse>> into a single Mono
        // then flatten and collect all articles into a single list
        return Flux.merge(newsRequests)
                .flatMap(Flux::fromIterable)
                .collectList();
    }

}