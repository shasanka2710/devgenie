package com.org.devgenie.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOrganization {
    private Long id;
    private String login;
    private String description;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("public_repos")
    private Integer publicRepos;

    @JsonProperty("public_members")
    private Integer publicMembers;

    private String name;
    private String email;
    private String blog;
    private String location;
    private String company;
}
