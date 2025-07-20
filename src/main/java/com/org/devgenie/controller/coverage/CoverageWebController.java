package com.org.devgenie.controller.coverage;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.coverage.RepositoryAnalysisRequest;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import com.org.devgenie.model.login.GitHubRepository;
import com.org.devgenie.service.coverage.CoverageAgentService;
import com.org.devgenie.service.coverage.RepositoryAnalysisService;
import com.org.devgenie.service.login.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequestMapping("/coverage")
@Slf4j
public class CoverageWebController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private CoverageAgentService coverageAgentService;

    @Autowired
    private RepositoryAnalysisService repositoryAnalysisService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    private static final ConcurrentHashMap<String, RepositoryAnalysisResponse> analysisCache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/analyze/{owner}/{repo}")
    public String analyzeRepository(@PathVariable String owner,
                                    @PathVariable String repo,
                                    Authentication authentication,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        log.info("Analyzing repository {}/{}", owner, repo);
        try {
            String accessToken = getAccessToken(authentication);
            if (accessToken == null) {
                redirectAttributes.addFlashAttribute("error", "Unable to retrieve access token");
                return "redirect:/dashboard";
            }

            // Get repository details
            Optional<GitHubRepository> repoOpt = gitHubService.getRepository(accessToken, owner, repo);
            if (repoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Repository not found: " + owner + "/" + repo);
                return "redirect:/dashboard";
            }

            GitHubRepository repository = repoOpt.get();
            String cacheKey = owner + "/" + repo;
            RepositoryAnalysisResponse analysis = fetchAnalysisResponseFromMongo(owner, repo, repository);
            if (analysis == null) {
                analysis = analysisCache.get(cacheKey);
                if (analysis == null) {
                    // Start async analysis
                    executor.submit(() -> {
                        RepositoryAnalysisRequest analysisRequest = new RepositoryAnalysisRequest();
                        analysisRequest.setRepositoryUrl(repository.getHtmlUrl());
                        analysisRequest.setGithubToken(accessToken);
                        analysisRequest.setBranch("main");
                        RepositoryAnalysisResponse result = repositoryAnalysisService.analyzeRepository(analysisRequest);
                        analysisCache.put(cacheKey, result);
                    });
                }
            }

            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);
            model.addAttribute("analysis", analysis); // will be null or ready

            return "repository-analysis";

        } catch (Exception e) {
            log.error("Error analyzing repository {}/{}", owner, repo, e);
            redirectAttributes.addFlashAttribute("error", "Failed to analyze repository: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    private RepositoryAnalysisResponse fetchAnalysisResponseFromMongo(String owner, String repo, GitHubRepository repository) {
        // Try to fetch from Mongo first
        RepositoryAnalysisResponse analysis = null;
        try{
            analysis = repositoryAnalysisService.getAnalysisFromMongo(repository.getHtmlUrl(), "main");
        }catch (CoverageDataNotFoundException ce){
            log.warn("No analysis found in Mongo for {}/{}: {}", owner, repo, ce.getMessage());
        }
        return analysis;
    }

    @GetMapping("/analyze/{owner}/{repo}/status")
    @ResponseBody
    public RepositoryAnalysisResponse getAnalysisStatus(@PathVariable String owner, @PathVariable String repo) {
        String cacheKey = owner + "/" + repo;
        return analysisCache.get(cacheKey);
    }

    @GetMapping("/dashboard/{owner}/{repo}")
    public String repositoryDashboard(@PathVariable String owner,
                                      @PathVariable String repo,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            String accessToken = getAccessToken(authentication);
            if (accessToken == null) {
                redirectAttributes.addFlashAttribute("error", "Unable to retrieve access token");
                return "redirect:/dashboard";
            }

            // Get repository details
            Optional<GitHubRepository> repoOpt = gitHubService.getRepository(accessToken, owner, repo);
            if (repoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Repository not found: " + owner + "/" + repo);
                return "redirect:/dashboard";
            }

            GitHubRepository repository = repoOpt.get();
            // Try to fetch from Mongo first
            RepositoryAnalysisResponse analysis = repositoryAnalysisService.getAnalysisFromMongo(repository.getHtmlUrl(), "main");
            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);
            model.addAttribute("analysis", analysis);

            return "repository-dashboard";

        } catch (Exception e) {
            log.error("Error loading repository dashboard {}/{}", owner, repo, e);
            redirectAttributes.addFlashAttribute("error", "Failed to load repository dashboard: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    private String getAccessToken(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return null;
        }

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauth2Token.getAuthorizedClientRegistrationId(),
                oauth2Token.getName()
        );

        return client != null ? client.getAccessToken().getTokenValue() : null;
    }
}
