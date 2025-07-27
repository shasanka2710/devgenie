/**
 * JavaScript client for File Coverage Improvement with WebSocket progress tracking
 * Add this to your repository dashboard or create a new page for file coverage improvement
 */

class FileCoverageClient {
    constructor() {
        this.websocket = null;
        this.sessionId = null;
        this.sessionType = null; // 'file' or 'repo'
        this.progressCallback = null;
    }

    /**
     * Start file coverage improvement process (async)
     */
    async improveFileCoverage(request, sessionId = null) {
        try {
            console.log('Starting async file coverage improvement for:', request.filePath);
            console.log('🔍 Frontend sessionId being sent:', sessionId);
            
            // If sessionId is provided, include it in the request
            const requestBody = sessionId ? { ...request, sessionId } : request;
            console.log('🔍 Request body:', requestBody);
            
            const response = await fetch('/api/coverage/file/improve-async', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            this.sessionId = result.sessionId;
            this.sessionType = 'file'; // Mark as file session
            
            console.log('File coverage improvement session started:', this.sessionId);
            console.log('🔍 Backend returned sessionId:', result.sessionId);
            
            // NOTE: WebSocket connection is handled by the frontend, not here
            
            return result;

        } catch (error) {
            console.error('Error starting file coverage improvement:', error);
            throw error;
        }
    }

    /**
     * Start repository coverage improvement process (async)
     */
    async improveRepositoryCoverage(request, sessionId = null) {
        try {
            console.log('Starting async repository coverage improvement for:', request.repositoryUrl);
            console.log('🔍 Frontend sessionId being sent:', sessionId);
            
            // If sessionId is provided, include it in the request
            const requestBody = sessionId ? { ...request, sessionId } : request;
            console.log('🔍 Request body:', requestBody);
            
            const response = await fetch('/api/coverage/repo/improve-async', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            this.sessionId = result.sessionId;
            this.sessionType = 'repo'; // Mark as repository session
            
            console.log('Repository coverage improvement session started:', this.sessionId);
            console.log('🔍 Backend returned sessionId:', result.sessionId);
            
            // NOTE: WebSocket connection is handled by the frontend, not here
            
            return result;

        } catch (error) {
            console.error('Error starting repository coverage improvement:', error);
            throw error;
        }
    }

    /**
     * Cancel a running session
     */
    async cancelSession(sessionId) {
        try {
            const response = await fetch(`/api/coverage/session/${sessionId}/cancel`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            
            // Close WebSocket if it's open
            if (this.websocket) {
                this.websocket.close();
            }
            
            return result;

        } catch (error) {
            console.error('Error cancelling session:', error);
            throw error;
        }
    }

    /**
     * Connect to WebSocket for real-time progress updates
     */
    connectToProgressWebSocket(sessionId) {
        const wsUrl = `ws://localhost:8080/ws/coverage-progress/${sessionId}`;
        console.log('Connecting to WebSocket:', wsUrl);
        
        this.websocket = new WebSocket(wsUrl);
        
        this.websocket.onopen = (event) => {
            console.log('WebSocket connected for session:', sessionId);
        };
        
        this.websocket.onmessage = (event) => {
            const progressUpdate = JSON.parse(event.data);
            console.log('Progress update:', progressUpdate);
            
            if (this.progressCallback) {
                this.progressCallback(progressUpdate);
            }
            
            // Update UI with progress
            this.updateProgressUI(progressUpdate);
        };
        
        this.websocket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
        
        this.websocket.onclose = (event) => {
            console.log('WebSocket connection closed:', event);
        };
    }

    /**
     * Update progress UI elements
     */
    updateProgressUI(progressUpdate) {
        // Update progress bar
        const progressBar = document.getElementById('coverage-progress-bar');
        if (progressBar) {
            progressBar.style.width = `${progressUpdate.progress}%`;
            progressBar.setAttribute('aria-valuenow', progressUpdate.progress);
        }

        // Update progress text
        const progressText = document.getElementById('coverage-progress-text');
        if (progressText) {
            progressText.textContent = `${progressUpdate.currentStep} (${progressUpdate.progress.toFixed(1)}%)`;
        }

        // Update status message
        const statusMessage = document.getElementById('coverage-status-message');
        if (statusMessage) {
            statusMessage.textContent = progressUpdate.message || progressUpdate.currentStep;
        }

        // Handle completion
        if (progressUpdate.progress >= 100) {
            this.handleCompletion(progressUpdate);
        }
    }

    /**
     * Handle completion of coverage improvement
     */
    handleCompletion(progressUpdate) {
        console.log('Coverage improvement completed!');
        
        // Close WebSocket connection
        if (this.websocket) {
            this.websocket.close();
        }

        // Show completion UI
        this.showCompletionResults();
    }

    /**
     * Get session status
     */
    async getSessionStatus(sessionId) {
        try {
            const response = await fetch(`/api/coverage/file/session/${sessionId}/status`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error getting session status:', error);
            throw error;
        }
    }

    /**
     * Apply generated changes
     */
    async applyChanges(sessionId, applyRequest) {
        try {
            // Determine the correct endpoint based on session type
            const endpoint = this.sessionType === 'repo' 
                ? `/api/coverage/repo/session/${sessionId}/apply`
                : `/api/coverage/file/session/${sessionId}/apply`;
                
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(applyRequest)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error applying changes:', error);
            throw error;
        }
    }

    /**
     * Show completion results and allow user to review/apply changes
     */
    async showCompletionResults() {
        try {
            const sessionStatus = await this.getSessionStatus(this.sessionId);
            const results = sessionStatus.results;
            
            // Show results modal
            this.showResultsModal(results);
            
        } catch (error) {
            console.error('Error showing completion results:', error);
        }
    }

    /**
     * Show results in a modal dialog
     */
    showResultsModal(results) {
        // Create modal HTML (this would be better implemented with your UI framework)
        const modalHtml = `
            <div class="modal fade" id="coverageResultsModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Coverage Improvement Results</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h6>File: ${results.fileName}</h6>
                                    <p><strong>Original Coverage:</strong> ${results.originalCoverage.toFixed(1)}%</p>
                                    <p><strong>Improved Coverage:</strong> ${results.improvedCoverage.toFixed(1)}%</p>
                                    <p><strong>Increase:</strong> +${results.coverageIncrease.toFixed(1)}%</p>
                                </div>
                                <div class="col-md-6">
                                    <h6>Generated Tests</h6>
                                    <p><strong>Tests Generated:</strong> ${results.totalTestsGenerated}</p>
                                    <p><strong>Processing Time:</strong> ${(results.processingTimeMs / 1000).toFixed(1)}s</p>
                                    <p><strong>Status:</strong> ${results.status}</p>
                                </div>
                            </div>
                            <div class="mt-3">
                                <h6>Test Files Created:</h6>
                                <ul>
                                    ${results.testFilePaths.map(path => `<li>${path}</li>`).join('')}
                                </ul>
                            </div>
                            <div class="mt-3">
                                <h6>Recommendations:</h6>
                                <ul>
                                    ${results.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                                </ul>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn btn-primary" onclick="fileCoverageClient.handleApplyChanges()">Apply Changes</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Add modal to page and show it
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('coverageResultsModal'));
        modal.show();
    }

    /**
     * Handle apply changes button click
     */
    async handleApplyChanges() {
        try {
            // Show spinner modal immediately
            this.showPRSpinnerModal();
            
            const applyRequest = {
                createPullRequest: true,
                prTitle: `Improve coverage for ${this.sessionId}`,
                prDescription: 'Generated tests to improve code coverage'
            };
            
            const result = await this.applyChanges(this.sessionId, applyRequest);
            
            // Hide spinner modal
            this.hidePRSpinnerModal();
            
            // Close current modal first
            const modal = bootstrap.Modal.getInstance(document.getElementById('coverageResultsModal'));
            modal.hide();
            
            // Show PR success modal if PR was created
            if (result.pullRequest && result.pullRequest.success) {
                this.showPullRequestSuccessModal(result.pullRequest);
            } else if (result.pullRequest && result.pullRequest.prUrl) {
                // Fallback - sometimes success flag might not be set but PR was created
                this.showPullRequestSuccessModal(result.pullRequest);
            } else {
                // Show error modal instead of alert
                const errorMsg = result.error || result.pullRequest?.error || 'Pull request creation failed';
                this.showPRErrorModal(errorMsg);
            }
            
        } catch (error) {
            // Hide spinner modal on error
            this.hidePRSpinnerModal();
            this.showPRErrorModal('Failed to create pull request: ' + error.message);
        }
    }

    /**
     * Show pull request success modal with PR details
     */
    showPullRequestSuccessModal(prData) {
        // Populate modal with PR data
        document.getElementById('prNumber').textContent = `#${prData.prNumber || 'N/A'}`;
        document.getElementById('prBranch').textContent = prData.branchName || 'N/A';
        document.getElementById('prSessionId').textContent = this.sessionId || 'N/A';
        document.getElementById('prDescription').textContent = 'Generated tests to improve code coverage';
        
        // Set up the PR link
        const prLink = document.getElementById('prLink');
        if (prData.prUrl) {
            prLink.href = prData.prUrl;
            prLink.style.display = 'inline-block';
        } else {
            prLink.style.display = 'none';
        }
        
        // Show the modal
        const prModal = new bootstrap.Modal(document.getElementById('prSuccessModal'));
        prModal.show();
    }

    /**
     * Show spinner modal during PR creation
     */
    showPRSpinnerModal() {
        // Remove any existing spinner modal first
        const existingSpinnerModal = document.getElementById('prSpinnerModal');
        if (existingSpinnerModal) {
            existingSpinnerModal.remove();
        }
        
        const spinnerModal = `
            <div class="modal fade" id="prSpinnerModal" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-body text-center py-5">
                            <div class="mb-4">
                                <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
                                    <span class="visually-hidden">Loading...</span>
                                </div>
                            </div>
                            <h5 class="mb-3">Creating Pull Request</h5>
                            <p class="text-muted mb-0">
                                <i class="bi bi-git me-2"></i>
                                Applying changes and creating your pull request...
                            </p>
                            <div class="mt-3">
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated" 
                                         role="progressbar" style="width: 100%"></div>
                                </div>
                            </div>
                            <p class="text-muted mt-3 small">This may take a few moments. Please do not close this window.</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', spinnerModal);
        const spinnerModalInstance = new bootstrap.Modal(document.getElementById('prSpinnerModal'));
        spinnerModalInstance.show();
    }

    /**
     * Hide spinner modal
     */
    hidePRSpinnerModal() {
        const spinnerModal = document.getElementById('prSpinnerModal');
        if (spinnerModal) {
            const modalInstance = bootstrap.Modal.getInstance(spinnerModal);
            if (modalInstance) {
                modalInstance.hide();
            }
            // Remove the modal from DOM after hiding
            setTimeout(() => {
                if (spinnerModal && spinnerModal.parentNode) {
                    spinnerModal.remove();
                }
            }, 300); // Wait for hide animation to complete
        }
    }

    /**
     * Show error modal for PR creation failures
     */
    showPRErrorModal(errorMessage) {
        // Remove any existing error modal first
        const existingErrorModal = document.getElementById('prErrorModal');
        if (existingErrorModal) {
            existingErrorModal.remove();
        }
        
        const errorModal = `
            <div class="modal fade" id="prErrorModal" tabindex="-1" data-bs-backdrop="static">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header bg-danger text-white">
                            <h5 class="modal-title"><i class="bi bi-exclamation-triangle me-2"></i> Pull Request Issue</h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div class="text-center mb-3">
                                <i class="bi bi-exclamation-circle-fill text-warning" style="font-size: 3rem;"></i>
                                <h4 class="mt-3">Changes Applied Successfully</h4>
                                <p class="text-muted">Your coverage improvement changes have been applied, but there was an issue with creating the pull request.</p>
                            </div>
                            
                            <div class="alert alert-warning">
                                <strong>Issue:</strong> ${errorMessage}
                            </div>
                            
                            <div class="alert alert-info">
                                <i class="bi bi-info-circle me-2"></i> 
                                <strong>What you can do:</strong>
                                <ul class="mb-0 mt-2">
                                    <li>Check your GitHub token permissions</li>
                                    <li>Manually create a pull request with the generated changes</li>
                                    <li>Contact support if the issue persists</li>
                                </ul>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary" data-bs-dismiss="modal">Understood</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', errorModal);
        const errorModalInstance = new bootstrap.Modal(document.getElementById('prErrorModal'));
        errorModalInstance.show();
    }

    /**
     * Set progress callback function
     */
    setProgressCallback(callback) {
        this.progressCallback = callback;
    }
}

// Global instance
const fileCoverageClient = new FileCoverageClient();

// Example usage functions
async function startFileCoverageImprovement(filePath) {
    const request = {
        repositoryUrl: getCurrentRepositoryUrl(),
        branch: getCurrentBranch(),
        filePath: filePath,
        targetCoverageIncrease: 25.0,
        maxTestsPerBatch: 5,
        validateTests: true,
        createPullRequest: false
    };

    try {
        const result = await fileCoverageClient.improveFileCoverage(request);
        console.log('File coverage improvement started:', result);
        
        // Show progress modal
        showProgressModal('Improving File Coverage');
        
    } catch (error) {
        console.error('Failed to start file coverage improvement:', error);
        alert('Failed to start file coverage improvement: ' + error.message);
    }
}

async function startRepositoryCoverageImprovement() {
    const request = {
        repositoryUrl: getCurrentRepositoryUrl(),
        branch: getCurrentBranch(),
        targetCoverageIncrease: 20.0,
        maxFilesToProcess: 50,
        excludePatterns: ['**/*Test.java', '**/test/**', '**/target/**'],
        forceRefresh: false
    };

    try {
        const result = await fileCoverageClient.improveRepositoryCoverage(request);
        console.log('Repository coverage improvement started:', result);
        
        // Show progress modal
        showProgressModal('Improving Repository Coverage');
        
    } catch (error) {
        console.error('Failed to start repository coverage improvement:', error);
        alert('Failed to start repository coverage improvement: ' + error.message);
    }
}

async function cancelCurrentSession() {
    if (fileCoverageClient.sessionId) {
        try {
            await fileCoverageClient.cancelSession(fileCoverageClient.sessionId);
            console.log('Session cancelled successfully');
            
            // Hide progress modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('coverageProgressModal'));
            if (modal) {
                modal.hide();
            }
            
        } catch (error) {
            console.error('Failed to cancel session:', error);
            alert('Failed to cancel session: ' + error.message);
        }
    }
}

// Helper functions (implement based on your application structure)
function getCurrentRepositoryUrl() {
    // Return current repository URL
    return window.location.pathname.includes('/coverage/') 
        ? 'https://github.com/user/repo' // Extract from URL or state
        : '';
}

function getCurrentBranch() {
    // Return current branch
    return 'main'; // Get from UI state or URL
}

function showProgressModal(title = 'Processing Coverage Improvement') {
    // Show progress modal HTML
    const progressModalHtml = `
        <div class="modal fade" id="coverageProgressModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${title}</h5>
                        <button type="button" class="btn-close" onclick="cancelCurrentSession()"></button>
                    </div>
                    <div class="modal-body">
                        <div class="progress mb-3">
                            <div id="coverage-progress-bar" class="progress-bar progress-bar-striped progress-bar-animated" 
                                 role="progressbar" style="width: 0%" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                        <p id="coverage-progress-text">Initializing...</p>
                        <p id="coverage-status-message" class="text-muted"></p>
                        <div class="d-flex justify-content-between align-items-center mt-3">
                            <small class="text-muted" id="coverage-session-id">Session: ${fileCoverageClient.sessionId || 'Not started'}</small>
                            <button type="button" class="btn btn-outline-danger btn-sm" onclick="cancelCurrentSession()">
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', progressModalHtml);
    const modal = new bootstrap.Modal(document.getElementById('coverageProgressModal'));
    modal.show();
}
