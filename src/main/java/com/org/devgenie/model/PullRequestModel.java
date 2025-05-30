package com.org.devgenie.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PullRequestModel {
    private int prCreatedCount;
    private int issuesResolved;
    private String engineeringTimeSaved;
    private String costSavings;
}
