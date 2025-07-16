package com.org.devgenie.service.login;

import com.org.devgenie.model.login.GitHubOrganization;
import com.org.devgenie.model.login.GitHubRepository;
import com.org.devgenie.model.login.GitHubUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GitHubService {

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${github.api.url}")
    private String githubApiBaseUrl;

    @Value("${github.api.timeout}")
    private Duration timeout;

    public GitHubService(WebClient.Builder webClientBuilder, OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.authorizedClientService = authorizedClientService;
    }

    public Optional<GitHubUser> getCurrentUser(String accessToken) {
        try {
            GitHubUser user = webClient
                    .get()
                    .uri(githubApiBaseUrl + "/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(GitHubUser.class)
                    .timeout(timeout)
                    .block();

            log.info("Successfully retrieved user: {}", user != null ? user.getLogin() : "null");
            return Optional.ofNullable(user);

        } catch (WebClientResponseException e) {
            log.error("Failed to fetch user from GitHub API: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching user from GitHub", e);
            return Optional.empty();
        }
    }

    public List<GitHubOrganization> getUserOrganizations(String accessToken) {
        try {
            List<GitHubOrganization> orgs = webClient
                    .get()
                    .uri(githubApiBaseUrl + "/user/orgs")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubOrganization>>() {})
                    .timeout(timeout)
                    .block();

            log.info("Successfully retrieved {} organizations", orgs != null ? orgs.size() : 0);
            return orgs != null ? orgs : List.of();

        } catch (WebClientResponseException e) {
            log.error("Failed to fetch organizations from GitHub API: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching organizations from GitHub", e);
            return List.of();
        }
    }

    public List<GitHubRepository> getUserRepositories(String accessToken) {
        try {
            List<GitHubRepository> repos = webClient
                    .get()
                    .uri(githubApiBaseUrl + "/user/repos?sort=updated&per_page=100&type=all")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {})
                    .timeout(timeout)
                    .block();

            log.info("Successfully retrieved {} repositories", repos != null ? repos.size() : 0);
            return repos != null ? repos : List.of();

        } catch (WebClientResponseException e) {
            log.error("Failed to fetch repositories from GitHub API: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching repositories from GitHub", e);
            return List.of();
        }
    }

    public List<GitHubRepository> getOrganizationRepositories(String accessToken, String orgName) {
        try {
            List<GitHubRepository> repos = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(githubApiBaseUrl + "/orgs/{org}/repos")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", "100")
                            .queryParam("type", "all")
                            .build(orgName))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubRepository>>() {})
                    .timeout(timeout)
                    .block();

            log.info("Successfully retrieved {} repositories for org: {}", repos != null ? repos.size() : 0, orgName);
            return repos != null ? repos : List.of();

        } catch (WebClientResponseException e) {
            log.error("Failed to fetch repositories for org {} from GitHub API: {}", orgName, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching repositories for org: {}", orgName, e);
            return List.of();
        }
    }

    public Optional<GitHubRepository> getRepository(String accessToken, String owner, String repo) {
        try {
            GitHubRepository repository = webClient
                    .get()
                    .uri(githubApiBaseUrl + "/repos/{owner}/{repo}", owner, repo)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(GitHubRepository.class)
                    .timeout(timeout)
                    .block();

            log.info("Successfully retrieved repository: {}/{}", owner, repo);
            return Optional.ofNullable(repository);

        } catch (WebClientResponseException e) {
            log.error("Failed to fetch repository {}/{} from GitHub API: {}", owner, repo, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching repository {}/{}", owner, repo, e);
            return Optional.empty();
        }
    }
}
