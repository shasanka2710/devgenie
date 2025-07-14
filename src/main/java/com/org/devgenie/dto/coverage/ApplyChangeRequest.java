package com.org.devgenie.dto.coverage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyChangesRequest {
    private String sessionId;
    private String repoPath;
    private List<FileChange> changes;
    private boolean createPullRequest = true;
}
