package com.org.devgenie.controller.login;

import com.org.devgenie.model.login.GitHubOrganization;
import com.org.devgenie.model.login.GitHubRepository;
import com.org.devgenie.model.login.GitHubUser;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class DashboardController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            String accessToken = getAccessToken(authentication);
            if (accessToken == null) {
                redirectAttributes.addFlashAttribute("error", "Unable to retrieve GitHub access token");
                return "redirect:/login";
            }

            // Get user profile
            Optional<GitHubUser> userOpt = gitHubService.getCurrentUser(accessToken);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Unable to retrieve GitHub user profile");
                return "redirect:/login";
            }

            GitHubUser user = userOpt.get();

            // Get organizations
            List<GitHubOrganization> organizations = gitHubService.getUserOrganizations(accessToken);

            // Get user repositories
            List<GitHubRepository> userRepos = gitHubService.getUserRepositories(accessToken);

            // Filter Java repositories
            List<GitHubRepository> javaRepos = userRepos.stream()
                    .filter(GitHubRepository::isJavaProject)
                    .collect(Collectors.toList());

            model.addAttribute("user", user);
            model.addAttribute("organizations", organizations);
            model.addAttribute("repositories", userRepos);
            model.addAttribute("javaRepositories", javaRepos);
            model.addAttribute("totalRepos", userRepos.size());
            model.addAttribute("javaReposCount", javaRepos.size());

            return "dashboard";

        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while loading your dashboard");
            return "redirect:/login";
        }
    }

    @GetMapping("/organization")
    public String getOrganizationRepos(@RequestParam String org, Authentication authentication, Model model) {
        try {
            String accessToken = getAccessToken(authentication);
            if (accessToken == null) {
                model.addAttribute("error", "Unable to retrieve access token");
                return "fragments/repository-list";
            }

            List<GitHubRepository> repos = gitHubService.getOrganizationRepositories(accessToken, org);
            List<GitHubRepository> javaRepos = repos.stream()
                    .filter(GitHubRepository::isJavaProject)
                    .collect(Collectors.toList());

            model.addAttribute("repositories", javaRepos);
            model.addAttribute("selectedOrg", org);

            return "fragments/repository-list";

        } catch (Exception e) {
            log.error("Error fetching organization repositories", e);
            model.addAttribute("error", "Failed to load repositories for organization: " + org);
            return "fragments/repository-list";
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
