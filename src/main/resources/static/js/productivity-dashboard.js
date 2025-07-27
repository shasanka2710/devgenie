/**
 * Productivity Dashboard JavaScript
 * Handles dashboard data loading, filtering, and interactive features
 */

class ProductivityDashboard {
    constructor() {
        this.currentFilter = {
            category: 'ALL',
            subCategory: 'ALL',
            status: 'ALL',
            timeRange: 'LAST_30_DAYS',
            page: 0,
            size: 20
        };
        this.chart = null;
        this.refreshInterval = null;
    }

    async init() {
        console.log('Initializing Productivity Dashboard...');
        
        // Clean up any existing instances first
        this.destroy();
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Load initial data
        await this.loadDashboardData();
        
        // Set up auto-refresh for active sessions
        this.startAutoRefresh();
    }

    setupEventListeners() {
        // Category filters
        const categoryFilter = document.getElementById('categoryFilter');
        const subCategoryFilter = document.getElementById('subCategoryFilter');
        const statusFilter = document.getElementById('statusFilter');
        const timeRangeFilter = document.getElementById('timeRangeFilter');

        if (categoryFilter) {
            categoryFilter.addEventListener('change', () => {
                this.currentFilter.category = categoryFilter.value;
                this.loadDashboardData();
            });
        }

        if (subCategoryFilter) {
            subCategoryFilter.addEventListener('change', () => {
                this.currentFilter.subCategory = subCategoryFilter.value;
                this.loadDashboardData();
            });
        }

        if (statusFilter) {
            statusFilter.addEventListener('change', () => {
                this.currentFilter.status = statusFilter.value;
                this.currentFilter.page = 0; // Reset pagination
                this.loadImprovementRecords();
            });
        }

        if (timeRangeFilter) {
            timeRangeFilter.addEventListener('change', () => {
                this.currentFilter.timeRange = timeRangeFilter.value;
                this.currentFilter.page = 0; // Reset pagination
                this.loadDashboardData();
            });
        }
    }

    async loadDashboardData() {
        try {
            this.showLoading();

            // Build API URL
            const params = new URLSearchParams({
                repositoryUrl: REPOSITORY_CONTEXT.repositoryUrl,
                branch: REPOSITORY_CONTEXT.branch,
                category: this.currentFilter.category,
                subCategory: this.currentFilter.subCategory,
                timeRange: this.currentFilter.timeRange
            });

            const response = await fetch(`/api/repository/${REPOSITORY_CONTEXT.repositoryId}/dashboard/summary?${params}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            
            if (this.hasData(data)) {
                this.populateDashboard(data);
                this.showContent();
            } else {
                this.showEmptyState();
            }

        } catch (error) {
            console.error('Error loading dashboard data:', error);
            this.showError('Failed to load dashboard data: ' + error.message);
        }
    }

    async loadImprovementRecords() {
        try {
            const params = new URLSearchParams({
                repositoryUrl: REPOSITORY_CONTEXT.repositoryUrl,
                branch: REPOSITORY_CONTEXT.branch,
                category: this.currentFilter.category,
                subCategory: this.currentFilter.subCategory,
                status: this.currentFilter.status,
                timeRange: this.currentFilter.timeRange,
                page: this.currentFilter.page,
                size: this.currentFilter.size,
                sortBy: 'DATE_DESC'
            });

            const response = await fetch(`/api/repository/${REPOSITORY_CONTEXT.repositoryId}/dashboard/records?${params}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            this.populateImprovementRecords(data);

        } catch (error) {
            console.error('Error loading improvement records:', error);
            this.showError('Failed to load improvement records: ' + error.message);
        }
    }

    hasData(data) {
        return data && (
            (data.totalSessions && parseInt(data.totalSessions.value) > 0) ||
            (data.recentActivity && data.recentActivity.length > 0) ||
            (data.activeSessions && data.activeSessions.length > 0)
        );
    }

    populateDashboard(data) {
        // Update metric cards
        this.updateMetricCards(data);
        
        // Update recent activity
        this.updateRecentActivity(data.recentActivity || []);
        
        // Update active sessions
        this.updateActiveSessions(data.activeSessions || []);
        
        // Update analytics chart
        this.updateTrendsChart(data.analytics);
        
        // Load improvement records
        this.loadImprovementRecords();
    }

    updateMetricCards(data) {
        const cards = {
            totalSessions: data.totalSessions,
            successRate: data.successRate,
            coverageIncrease: data.averageCoverageIncrease,
            timeSaved: data.timeSaved
        };

        Object.entries(cards).forEach(([key, card]) => {
            if (card) {
                this.updateMetricCard(key, card);
            }
        });
    }

    updateMetricCard(key, card) {
        const valueElement = document.getElementById(`${key}Value`);
        const subtitleElement = document.getElementById(`${key}Subtitle`);
        const changeElement = document.getElementById(`${key}Change`);

        if (valueElement) valueElement.textContent = card.value;
        if (subtitleElement) subtitleElement.textContent = card.subtitle;
        
        if (changeElement && card.changePercentage !== undefined) {
            const direction = card.changeDirection || 'stable';
            const icon = direction === 'up' ? '↗' : direction === 'down' ? '↙' : '→';
            const color = direction === 'up' ? '#28a745' : direction === 'down' ? '#dc3545' : '#6c757d';
            
            changeElement.innerHTML = `<span style="color: ${color}">${icon} ${Math.abs(card.changePercentage).toFixed(1)}% vs last week</span>`;
        }
    }

    updateRecentActivity(activities) {
        const container = document.getElementById('recentActivityList');
        if (!container) return;

        if (activities.length === 0) {
            container.innerHTML = `
                <div class="text-center py-3 text-muted">
                    <i class="bi bi-clock-history"></i>
                    <p class="mb-0 mt-2">No recent activity</p>
                </div>
            `;
            return;
        }

        container.innerHTML = activities.slice(0, 10).map(activity => `
            <div class="timeline-item">
                <div class="timeline-marker ${this.getStatusColor(activity.status)}">
                    <i class="bi ${this.getStatusIcon(activity.status)}"></i>
                </div>
                <div class="timeline-content">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h6 class="mb-1">${activity.description}</h6>
                            <small class="text-muted">
                                ${activity.filePath ? `<code>${activity.filePath.split('/').pop()}</code> • ` : ''}
                                ${activity.timestamp}
                            </small>
                        </div>
                        <div class="text-end">
                            ${activity.coverageIncrease ? `
                                <span class="badge bg-success">+${activity.coverageIncrease.toFixed(1)}%</span>
                            ` : ''}
                            ${activity.testsGenerated ? `
                                <small class="text-muted d-block">${activity.testsGenerated} tests</small>
                            ` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
    }

    updateActiveSessions(sessions) {
        const container = document.getElementById('activeSessionsList');
        if (!container) return;

        if (sessions.length === 0) {
            container.innerHTML = `
                <div class="text-center py-3 text-muted">
                    <i class="bi bi-check-circle"></i>
                    <p class="mb-0 mt-2">No active sessions</p>
                </div>
            `;
            return;
        }

        container.innerHTML = sessions.map(session => `
            <div class="border-bottom pb-3 mb-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <div>
                        <h6 class="mb-1">${session.type.replace('_', ' ')}</h6>
                        <small class="text-muted">
                            ${session.filePath ? session.filePath.split('/').pop() : 'Repository-wide'}
                        </small>
                    </div>
                    <span class="badge ${this.getStatusColor(session.status)}">${session.status}</span>
                </div>
                
                ${session.progress !== undefined ? `
                    <div class="progress mb-2" style="height: 6px;">
                        <div class="progress-bar" style="width: ${session.progress}%"></div>
                    </div>
                    <small class="text-muted">${session.currentStep}</small>
                ` : ''}
                
                ${session.canModify ? `
                    <div class="mt-2">
                        <button class="btn btn-outline-primary btn-sm" onclick="modifySession('${session.sessionId}')">
                            <i class="bi bi-pencil"></i> Modify
                        </button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    populateImprovementRecords(pageData) {
        const container = document.getElementById('improvementRecords');
        if (!container) return;

        const records = pageData.content || [];
        
        // Filter out null records
        const validRecords = records.filter(record => record !== null && record !== undefined);
        
        if (validRecords.length === 0) {
            container.innerHTML = `
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-table"></i>
                    <p class="mb-0 mt-2">No improvement records found</p>
                </div>
            `;
            return;
        }

        const tableHtml = `
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>File/Type</th>
                            <th>Coverage</th>
                            <th>Tests</th>
                            <th>Time</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${validRecords.map(record => `
                            <tr>
                                <td>
                                    <div>
                                        <strong>${record.fileName || record.type || 'Unknown'}</strong>
                                        <br>
                                        <small class="text-muted">${record.startedAt || 'Unknown time'}</small>
                                    </div>
                                </td>
                                <td>
                                    ${record.coverageIncrease !== null && record.coverageIncrease !== undefined ? `
                                        <div>
                                            <span class="badge bg-success">+${this.safeToFixed(record.coverageIncrease, 1)}%</span>
                                            <br>
                                            <small class="text-muted">${this.safeToFixed(record.originalCoverage, 1)}% → ${this.safeToFixed(record.improvedCoverage, 1)}%</small>
                                        </div>
                                    ` : '<span class="text-muted">N/A</span>'}
                                </td>
                                <td>
                                    ${record.totalTestsGenerated || 0}
                                    ${record.validation?.testsExecuted ? `
                                        <br><small class="text-muted">${record.validation.testsPassed || 0}/${record.validation.testsExecuted} passed</small>
                                    ` : ''}
                                </td>
                                <td>
                                    ${record.processingTimeMs ? this.formatDuration(record.processingTimeMs) : 'N/A'}
                                </td>
                                <td>
                                    <span class="badge ${this.getStatusColor(record.status || 'UNKNOWN')}">${record.status || 'UNKNOWN'}</span>
                                </td>
                                <td>
                                    <button class="btn btn-outline-secondary btn-sm" onclick="viewSessionDetails('${record.sessionId || ''}')">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                    ${this.canModifyRecord(record) ? `
                                        <button class="btn btn-outline-primary btn-sm ms-1" onclick="modifySession('${record.sessionId || ''}')">
                                            <i class="bi bi-pencil"></i>
                                        </button>
                                    ` : ''}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
            
            ${pageData.totalPages > 1 ? this.createPagination(pageData) : ''}
        `;

        container.innerHTML = tableHtml;
    }

    updateTrendsChart(analytics) {
        const canvas = document.getElementById('trendsChart');
        if (!canvas || !analytics) return;

        // Destroy existing chart instance completely
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }

        // Clear any existing Chart.js instances from the canvas
        if (Chart.getChart(canvas)) {
            Chart.getChart(canvas).destroy();
        }

        const ctx = canvas.getContext('2d');
        
        // Clear the canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Create new chart with error handling
        try {
            this.chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: analytics.coverageTrends?.map(trend => trend.date) || [],
                    datasets: [
                        {
                            label: 'Coverage Increase %',
                            data: analytics.coverageTrends?.map(trend => trend.value) || [],
                            borderColor: 'var(--wf-red)',
                            backgroundColor: 'rgba(215, 25, 33, 0.1)',
                            tension: 0.4,
                            fill: true
                        },
                        {
                            label: 'Success Rate %',
                            data: analytics.successRateTrends?.map(trend => trend.value) || [],
                            borderColor: 'var(--wf-green)',
                            backgroundColor: 'rgba(56, 142, 60, 0.1)',
                            tension: 0.4,
                            fill: false
                        }
                    ]
                },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            font: {
                                size: 11
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: 'rgba(0,0,0,0.1)'
                        },
                        ticks: {
                            font: {
                                size: 10
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                size: 10
                            },
                            maxTicksLimit: 7
                        }
                    }
                }
            }
        });
        
        console.log('Trends chart created successfully');
        
        } catch (error) {
            console.error('Error creating trends chart:', error);
            // Show empty chart message
            const chartContainer = canvas.parentElement;
            chartContainer.innerHTML = `
                <div class="text-center text-muted py-4">
                    <i class="bi bi-graph-up" style="font-size: 2rem;"></i>
                    <p class="mt-2">Unable to load chart</p>
                </div>
            `;
        }
    }

    createPagination(pageData) {
        const currentPage = pageData.number;
        const totalPages = pageData.totalPages;
        
        let pagination = '<nav class="mt-3"><ul class="pagination pagination-sm justify-content-center">';
        
        // Previous button
        pagination += `
            <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="dashboardInstance.goToPage(${currentPage - 1})">Previous</a>
            </li>
        `;
        
        // Page numbers (show max 5 pages)
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages, startPage + 5);
        
        for (let i = startPage; i < endPage; i++) {
            pagination += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="dashboardInstance.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }
        
        // Next button
        pagination += `
            <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="dashboardInstance.goToPage(${currentPage + 1})">Next</a>
            </li>
        `;
        
        pagination += '</ul></nav>';
        return pagination;
    }

    goToPage(page) {
        this.currentFilter.page = page;
        this.loadImprovementRecords();
    }

    // Helper methods
    getStatusColor(status) {
        const statusColors = {
            'COMPLETED': 'bg-success',
            'READY_FOR_REVIEW': 'bg-success',
            'PROCESSING': 'bg-primary',
            'ANALYZING': 'bg-info',
            'GENERATING_TESTS': 'bg-info',
            'FAILED': 'bg-danger',
            'ERROR': 'bg-danger',
            'CREATED': 'bg-secondary',
            'INITIALIZING': 'bg-secondary'
        };
        return statusColors[status] || 'bg-secondary';
    }

    getStatusIcon(status) {
        const statusIcons = {
            'COMPLETED': 'bi-check-circle',
            'READY_FOR_REVIEW': 'bi-check-circle',
            'PROCESSING': 'bi-gear',
            'ANALYZING': 'bi-search',
            'GENERATING_TESTS': 'bi-code',
            'FAILED': 'bi-x-circle',
            'ERROR': 'bi-exclamation-triangle',
            'CREATED': 'bi-circle',
            'INITIALIZING': 'bi-clock'
        };
        return statusIcons[status] || 'bi-circle';
    }

    formatDuration(milliseconds) {
        if (!milliseconds) return 'N/A';
        
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        
        if (hours > 0) {
            return `${hours}h ${minutes % 60}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    canModifyRecord(record) {
        return record && ['FAILED', 'ERROR', 'CREATED'].includes(record.status);
    }

    safeToFixed(value, decimals) {
        if (value === null || value === undefined || isNaN(value)) {
            return '0';
        }
        return Number(value).toFixed(decimals);
    }

    // UI State Management
    showLoading() {
        const loading = document.getElementById('dashboardLoading');
        const content = document.getElementById('dashboardContent');
        const emptyState = document.getElementById('dashboardEmptyState');
        
        if (loading) loading.style.display = 'block';
        if (content) content.style.display = 'none';
        if (emptyState) emptyState.style.display = 'none';
    }

    showContent() {
        const loading = document.getElementById('dashboardLoading');
        const content = document.getElementById('dashboardContent');
        const emptyState = document.getElementById('dashboardEmptyState');
        
        if (loading) loading.style.display = 'none';
        if (content) content.style.display = 'block';
        if (emptyState) emptyState.style.display = 'none';
    }

    showEmptyState() {
        const loading = document.getElementById('dashboardLoading');
        const content = document.getElementById('dashboardContent');
        const emptyState = document.getElementById('dashboardEmptyState');
        
        if (loading) loading.style.display = 'none';
        if (content) content.style.display = 'none';
        if (emptyState) emptyState.style.display = 'block';
    }

    showError(message) {
        const container = document.getElementById('dashboardContent');
        if (container) {
            container.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle me-2"></i>
                    ${message}
                </div>
            `;
        }
        this.showContent();
    }

    startAutoRefresh() {
        // Refresh active sessions every 30 seconds
        this.refreshInterval = setInterval(() => {
            this.loadDashboardData();
        }, 30000);
    }

    stopAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    }

    destroy() {
        console.log('Destroying ProductivityDashboard instance...');
        this.stopAutoRefresh();
        
        // Destroy chart instance completely
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }

        // Also clear any remaining Chart.js instances on the canvas
        const canvas = document.getElementById('trendsChart');
        if (canvas && Chart.getChart(canvas)) {
            Chart.getChart(canvas).destroy();
        }
    }
}

// Global functions for UI interactions
function refreshDashboard() {
    if (window.dashboardInstance) {
        console.log('Refreshing dashboard via global function...');
        window.dashboardInstance.loadDashboardData();
    } else {
        console.warn('No dashboard instance available for refresh');
    }
}

function modifySession(sessionId) {
    // TODO: Implement session modification modal
    alert(`Session modification functionality will be implemented in future iterations.\nSession ID: ${sessionId}`);
}

function viewSessionDetails(sessionId) {
    // TODO: Implement session details modal
    alert(`Session details modal will be implemented in future iterations.\nSession ID: ${sessionId}`);
}

// Clean up when navigating away
window.addEventListener('beforeunload', function() {
    if (window.dashboardInstance) {
        window.dashboardInstance.destroy();
    }
});
