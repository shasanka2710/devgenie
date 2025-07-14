package com.org.devgenie.dto.coverage;

import com.org.devgenie.model.coverage.FileChange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyChangesRequest {
    private String sessionId;
    private String repoPath;
    private List<FileChange> changes;
    private boolean createPullRequest = true;
}
