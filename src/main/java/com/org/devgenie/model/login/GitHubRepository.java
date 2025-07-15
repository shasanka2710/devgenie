package com.org.devgenie.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepository {
    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    private String description;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("clone_url")
    private String cloneUrl;

    @JsonProperty("ssh_url")
    private String sshUrl;

    @JsonProperty("default_branch")
    private String defaultBranch;

    private String language;

    @JsonProperty("stargazers_count")
    private Integer stargazersCount;

    @JsonProperty("watchers_count")
    private Integer watchersCount;

    @JsonProperty("forks_count")
    private Integer forksCount;

    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("pushed_at")
    private String pushedAt;

    private Long size;

    private GitHubUser owner;

    public boolean isJavaProject() {
        return "Java".equalsIgnoreCase(language);
    }

    public String getVisibilityBadge() {
        return isPrivate ? "Private" : "Public";
    }

    public String getVisibilityBadgeClass() {
        return isPrivate ? "badge-danger" : "badge-success";
    }
}
