package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceStatusResponse {
    private String workspaceId;
    private String workspaceDir;
    private String status; // ACTIVE, NOT_FOUND, ERROR
    private List<String> repositories;
    private LocalDateTime lastAccessed;
    private String error;

    public static WorkspaceStatusResponse error(String error) {
        return WorkspaceStatusResponse.builder()
                .status("ERROR")
                .error(error)
                .build();
    }
}
