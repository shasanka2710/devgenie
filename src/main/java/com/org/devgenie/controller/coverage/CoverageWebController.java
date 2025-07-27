package com.org.devgenie.controller.coverage;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.coverage.RepositoryAnalysisRequest;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import com.org.devgenie.model.login.GitHubRepository;
import com.org.devgenie.service.coverage.CoverageAgentService;
import com.org.devgenie.service.coverage.FastDashboardService;
import com.org.devgenie.service.coverage.RepositoryAnalysisService;
import com.org.devgenie.service.coverage.RepositoryDashboardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
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
    private RepositoryDashboardService repositoryDashboardService;

    @Autowired
    private FastDashboardService fastDashboardService;

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
                        
                        // Perform repository analysis
                        RepositoryAnalysisResponse result = repositoryAnalysisService.analyzeRepository(analysisRequest);
                        analysisCache.put(cacheKey, result);
                        
                        // Dashboard cache generation is now automatically triggered in RepositoryAnalysisService
                        log.info("Repository analysis completed, dashboard cache generation triggered automatically");
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
        log.debug("Checking analysis status for cache key: {}", cacheKey);
        
        RepositoryAnalysisResponse cachedResponse = analysisCache.get(cacheKey);
        
        if (cachedResponse != null) {
            log.debug("Found cached response for {}: success={}", cacheKey, cachedResponse.isSuccess());
            // Return the cached response (analysis completed)
            return cachedResponse;
        } else {
            log.debug("No cached response found for {}, returning in-progress status", cacheKey);
            // Analysis not found in cache - either not started or still in progress
            // Return a default "in progress" response instead of null
            return RepositoryAnalysisResponse.builder()
                    .success(false)
                    .error("Analysis in progress or not started")
                    .build();
        }
    }

    @GetMapping("/dashboard/{owner}/{repo}")
    public String repositoryDashboard(@PathVariable String owner,
                                      @PathVariable String repo,
                                      @RequestParam(value = "forceRefresh", required = false) String forceRefresh,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        log.info("=== REPOSITORY DASHBOARD REQUEST ===");
        log.info("Owner: {}, Repo: {}, ForceRefresh: {}", owner, repo, forceRefresh);
        log.info("Authentication: {}", authentication != null ? "EXISTS" : "NULL");
        try {
            String accessToken = null;
            
            // For testing purposes, allow null authentication
            if (authentication != null) {
                accessToken = getAccessToken(authentication);
                if (accessToken == null) {
                    redirectAttributes.addFlashAttribute("error", "Unable to retrieve access token");
                    return "redirect:/dashboard";
                }
            } else {
                log.info("No authentication provided - proceeding with null access token for testing");
            }

            // Get repository details (skip for testing if no auth)
            GitHubRepository repository = null;
            if (accessToken != null) {
                Optional<GitHubRepository> repoOpt = gitHubService.getRepository(accessToken, owner, repo);
                if (repoOpt.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Repository not found: " + owner + "/" + repo);
                    return "redirect:/dashboard";
                }
                repository = repoOpt.get();
            } else {
                // Create a mock repository for testing
                repository = new GitHubRepository();
                repository.setName(repo);
                repository.setFullName(owner + "/" + repo);
                repository.setHtmlUrl("https://github.com/" + owner + "/" + repo);
            }
            
            // ENHANCED DASHBOARD LOADING - With cache validation and recovery
            log.info("Loading dashboard with validation from cache for fast response: {}", repository.getHtmlUrl());
            
            // Check for force refresh
            if ("true".equals(forceRefresh)) {
                log.info("FORCE REFRESH REQUESTED - clearing and rebuilding cache");
                fastDashboardService.forceCacheRefresh(repository.getHtmlUrl(), "main");
            }
            
            RepositoryDashboardService.DashboardData dashboardData = 
                fastDashboardService.getFastDashboardDataWithValidation(repository.getHtmlUrl(), "main");
            
            // Try to fetch from Mongo first (for backward compatibility)
            RepositoryAnalysisResponse analysis = null;
            try {
                analysis = repositoryAnalysisService.getAnalysisFromMongo(repository.getHtmlUrl(), "main");
            } catch (Exception e) {
                log.warn("No analysis found in Mongo for {}", repository.getHtmlUrl());
            }
            
            // DEBUG: Log dashboard data details
            log.info("=== DASHBOARD DEBUG INFO ===");
            log.info("Dashboard data: {}", dashboardData != null ? "EXISTS" : "NULL");
            log.info("File tree: {}", dashboardData.getFileTree() != null ? "EXISTS" : "NULL");
            if (dashboardData.getFileTree() != null) {
                log.info("File tree name: {}", dashboardData.getFileTree().getName());
                log.info("File tree type: {}", dashboardData.getFileTree().getType());
                log.info("File tree has children: {}", dashboardData.getFileTree().hasChildren());
                log.info("File tree children count: {}", dashboardData.getFileTree().getChildren().size());
                
                // FULL tree logging to debug nesting issue
                logFullTreeStructure(dashboardData.getFileTree(), 0);
            }
            log.info("File details count: {}", dashboardData.getFileDetails().size());
            log.info("=== END DEBUG INFO ===");
            
            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);
            model.addAttribute("analysis", analysis);
            model.addAttribute("dashboardData", dashboardData);
            model.addAttribute("overallMetrics", dashboardData.getOverallMetrics());
            model.addAttribute("fileTree", dashboardData.getFileTree());
            model.addAttribute("fileDetails", dashboardData.getFileDetails());
            
            // Add repository context for coverage improvement
            model.addAttribute("repositoryUrl", repository.getHtmlUrl());
            model.addAttribute("defaultBranch", repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main");
            model.addAttribute("fullName", repository.getFullName()); // owner/repo format

            return "repository-dashboard";

        } catch (Exception e) {
            log.error("Error loading repository dashboard {}/{}", owner, repo, e);
            redirectAttributes.addFlashAttribute("error", "Failed to load repository dashboard: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Unified repository page with tabbed interface for Insights, Coverage, and Issues
     */
    @GetMapping("/repository/{owner}/{repo}")
    public String unifiedRepositoryPage(@PathVariable String owner,
                                       @PathVariable String repo,
                                       @RequestParam(value = "tab", defaultValue = "insights") String activeTab,
                                       @RequestParam(value = "forceRefresh", required = false) String forceRefresh,
                                       Authentication authentication,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        log.info("=== UNIFIED REPOSITORY PAGE REQUEST ===");
        log.info("Owner: {}, Repo: {}, Tab: {}, ForceRefresh: {}", owner, repo, activeTab, forceRefresh);
        
        try {
            final String accessToken;
            
            if (authentication != null) {
                accessToken = getAccessToken(authentication);
                if (accessToken == null) {
                    redirectAttributes.addFlashAttribute("error", "Unable to retrieve access token");
                    return "redirect:/dashboard";
                }
            } else {
                accessToken = null;
            }

            // Get repository details
            Optional<GitHubRepository> repoOpt = Optional.empty();
            if (accessToken != null) {
                repoOpt = gitHubService.getRepository(accessToken, owner, repo);
            }
            
            GitHubRepository repository;
            if (repoOpt.isPresent()) {
                repository = repoOpt.get();
            } else {
                // Create minimal repository object for public repos or testing
                repository = new GitHubRepository();
                repository.setName(repo);
                repository.setFullName(owner + "/" + repo);
                repository.setHtmlUrl("https://github.com/" + owner + "/" + repo);
            }

            // Fetch analysis data for Insights tab
            RepositoryAnalysisResponse analysis = null;
            String cacheKey = owner + "/" + repo;
            try {
                analysis = repositoryAnalysisService.getAnalysisFromMongo(repository.getHtmlUrl(), "main");
            } catch (Exception e) {
                log.warn("No analysis found in Mongo for {}", repository.getHtmlUrl());
                // Check cache for ongoing analysis
                analysis = analysisCache.get(cacheKey);
            }

            // If no analysis exists and we're on insights tab, start analysis
            if (analysis == null && "insights".equals(activeTab)) {
                log.info("Starting background analysis for {}/{}", owner, repo);
                executor.submit(() -> {
                    try {
                        RepositoryAnalysisRequest analysisRequest = new RepositoryAnalysisRequest();
                        analysisRequest.setRepositoryUrl(repository.getHtmlUrl());
                        analysisRequest.setGithubToken(accessToken);
                        analysisRequest.setBranch("main");
                        
                        log.info("Executing analysis for cache key: {}", cacheKey);
                        RepositoryAnalysisResponse result = repositoryAnalysisService.analyzeRepository(analysisRequest);
                        
                        log.info("Analysis completed, storing in cache with key: {} - Success: {}", cacheKey, result.isSuccess());
                        analysisCache.put(cacheKey, result);
                        log.info("Cache updated successfully for key: {}", cacheKey);
                        log.info("Repository analysis completed for unified page");
                    } catch (Exception e) {
                        log.error("Failed to complete background analysis for {}/{}", owner, repo, e);
                        // Store error result in cache so polling knows it failed
                        RepositoryAnalysisResponse errorResult = RepositoryAnalysisResponse.error("Analysis failed: " + e.getMessage());
                        analysisCache.put(cacheKey, errorResult);
                    }
                });
            }

            // Fetch dashboard data for Coverage tab
            RepositoryDashboardService.DashboardData dashboardData = null;
            if ("coverage".equals(activeTab) || analysis != null) {
                try {
                    if ("true".equals(forceRefresh)) {
                        log.info("FORCE REFRESH REQUESTED - clearing and rebuilding cache");
                        fastDashboardService.forceCacheRefresh(repository.getHtmlUrl(), "main");
                    }
                    
                    dashboardData = fastDashboardService.getFastDashboardDataWithValidation(repository.getHtmlUrl(), "main");
                } catch (Exception e) {
                    log.error("Error loading dashboard data: {}", e.getMessage());
                }
            }

            // For dashboard tab, we need repository context but not necessarily coverage data
            if ("dashboard".equals(activeTab)) {
                log.info("Dashboard tab requested for {}/{}", owner, repo);
                // The ProductivityDashboardController will handle the actual dashboard API calls
            }

            // Set model attributes for unified page
            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);
            model.addAttribute("activeTab", activeTab);
            model.addAttribute("analysis", analysis);
            
            if (dashboardData != null) {
                model.addAttribute("dashboardData", dashboardData);
                model.addAttribute("overallMetrics", dashboardData.getOverallMetrics());
                model.addAttribute("fileTree", dashboardData.getFileTree());
                model.addAttribute("fileDetails", dashboardData.getFileDetails());
            }
            
            // Repository context for all tabs
            model.addAttribute("repositoryUrl", repository.getHtmlUrl());
            model.addAttribute("defaultBranch", repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main");
            model.addAttribute("fullName", repository.getFullName());

            return "repository-unified";

        } catch (Exception e) {
            log.error("Error loading unified repository page {}/{}", owner, repo, e);
            redirectAttributes.addFlashAttribute("error", "Failed to load repository page: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Manual cache refresh endpoint
     */
    @GetMapping("/cache/refresh/{owner}/{repo}")
    @ResponseBody
    public Map<String, Object> refreshCache(@PathVariable String owner, @PathVariable String repo, Authentication authentication) {
        try {
            String repoUrl = "https://github.com/" + owner + "/" + repo;
            
            log.info("Manual cache refresh requested for: {}", repoUrl);
            fastDashboardService.forceCacheRefresh(repoUrl, "main");
            
            return Map.of(
                "status", "success",
                "message", "Cache refreshed successfully",
                "repository", repoUrl
            );
        } catch (Exception e) {
            log.error("Cache refresh failed for {}/{}", owner, repo, e);
            return Map.of(
                "status", "error",
                "message", "Cache refresh failed: " + e.getMessage()
            );
        }
    }

    /**
     * Cache health status endpoint
     */
    @GetMapping("/cache/status/{owner}/{repo}")
    @ResponseBody
    public Map<String, Object> getCacheStatus(@PathVariable String owner, @PathVariable String repo) {
        try {
            String repoUrl = "https://github.com/" + owner + "/" + repo;
            
            Map<String, Object> healthStatus = fastDashboardService.getCacheHealthStatus(repoUrl, "main");
            healthStatus.put("repository", repoUrl);
            healthStatus.put("owner", owner);
            healthStatus.put("repo", repo);
            
            return healthStatus;
        } catch (Exception e) {
            log.error("Failed to get cache status for {}/{}", owner, repo, e);
            return Map.of(
                "status", "error",
                "message", "Failed to get cache status: " + e.getMessage()
            );
        }
    }

    /**
     * Coverage improvement page
     */
    @GetMapping("/improvement")
    public String coverageImprovementPage(Model model, Authentication authentication) {
        try {
            // Add user context if needed
            if (authentication != null) {
                model.addAttribute("user", authentication.getName());
            }
            
            return "coverage-improvement";
        } catch (Exception e) {
            log.error("Error loading coverage improvement page", e);
            model.addAttribute("error", "Failed to load coverage improvement page");
            return "error";
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

    /**
     * Helper method to log the full tree structure for debugging
     */
    private void logFullTreeStructure(RepositoryDashboardService.FileTreeNode node, int depth) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        log.info("{}|- {} ({}) - {} children", indent, node.getName(), node.getType(), 
                node.hasChildren() ? node.getChildren().size() : 0);
        
        if (node.hasChildren()) {
            for (RepositoryDashboardService.FileTreeNode child : node.getChildren()) {
                logFullTreeStructure(child, depth + 1);
            }
        }
    }
}
