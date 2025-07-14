package com.org.devgenie.model.coverage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestResult {
    private int prNumber;
    private String prUrl;
    private String branchName;
    private boolean success;
    private String error;
}
