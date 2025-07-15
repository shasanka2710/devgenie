package com.org.devgenie.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubUser {
    private Long id;
    private String login;
    private String name;
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("public_repos")
    private Integer publicRepos;

    @JsonProperty("public_gists")
    private Integer publicGists;

    private Integer followers;
    private Integer following;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private String bio;
    private String location;
    private String company;
    private String blog;
}
