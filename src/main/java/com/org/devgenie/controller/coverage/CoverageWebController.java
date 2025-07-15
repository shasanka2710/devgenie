package com.org.devgenie.controller.coverage;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

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

    @GetMapping("/analyze/{owner}/{repo}")
    public String analyzeRepository(@PathVariable String owner,
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

            // TODO: Analyze repository using your existing service
            // RepositoryAnalysisResponse analysis = repositoryAnalysisService.analyzeRepository(...);

            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);
            // model.addAttribute("analysis", analysis);

            return "repository-analysis";

        } catch (Exception e) {
            log.error("Error analyzing repository {}/{}", owner, repo, e);
            redirectAttributes.addFlashAttribute("error", "Failed to analyze repository: " + e.getMessage());
            return "redirect:/dashboard";
        }
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

            model.addAttribute("repository", repository);
            model.addAttribute("owner", owner);
            model.addAttribute("repoName", repo);

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
