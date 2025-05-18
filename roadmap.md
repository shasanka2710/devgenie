# 🛠️ Project Plan: AI-Powered Sonar Issue Resolution System

## 1. ✅ Current System (POC) Overview

### Technology Stack:
- **Backend**: Spring Boot
- **Frontend**: Thymeleaf
- **Database**: MongoDB
- **Integrations**: GitHub API, SonarQube REST API, LLM (e.g., Gemini Vertex AI)

### Flow:
1. GitHub repository URL, SonarQube component key, and access tokens are configured in `application.properties`.
2. On application start:
    - The GitHub repository is cloned.
    - On restart, it fetches the latest changes.
3. When the user navigates to the *Insights* page:
    - A REST API call is made to SonarQube using the configured token.
    - Issues are fetched and stored in MongoDB.
    - Issues are displayed in the UI via a tabular web view.
4. When the user clicks **Apply Fix**:
    - UI sends file path and issue description to the backend.
    - Application reads the file from the cloned repo and constructs a prompt for the LLM using the issue + file context.
    - LLM returns fixed code which is applied to the file.
    - Git integration creates a feature branch and opens a Pull Request automatically.

### ✅ Benefits of Current System

| Area                  | Benefit                                                                 |
|-----------------------|-------------------------------------------------------------------------|
| **End-to-End Flow**    | Complete pipeline from issue detection to PR creation is implemented.   |
| **LLM Integration**    | Prompt construction and fix application is functional.                  |
| **GitHub Automation**  | Automatically creates PRs with AI-generated code fixes.                 |
| **UI Insights**        | Clean tabular display of SonarQube issues integrated with MongoDB.      |
| **Modular Backend**    | Separate components for Git, Sonar, and LLM allow easier future scaling.|

---

## 2. ⚠️ Limitations of Current System

| Area                 | Limitation                                               |
|----------------------|----------------------------------------------------------|
| **Scalability**       | One repository per instance; cannot scale to 100K+ repos |
| **Security**          | Secrets stored in `application.properties`               |
| **Onboarding**        | Manual configuration for every repository                |
| **Extensibility**     | Configuration hardcoded;
| **Developer UX**      | Not embedded within developer IDE workflows              |

---

## 3. 🧩 Option 1: Developer IDE Plugin

### How It Works:
- Plugin for IntelliJ or VSCode.
- Fetches issues from SonarQube API or IDE's inspection results.
- Sends file and issue to LLM, gets fix.
- Applies fix directly to code.
- Offers Git integration to commit and push changes or create a PR.

### Benefits:
- Lightweight and easy to distribute.
- Quick adoption by developers.
- No backend infrastructure or onboarding portal needed.
- Keeps credentials local on the developer’s machine.
- Improves developer productivity significantly.

### Drawbacks:
- No org-wide governance or visibility.
- Cannot perform bulk fixes or automation.
- Manual usage — developers must invoke it per file/issue.
- No audit or analytics at org level.

---

## 4. 🏢 Option 2: Scalable Multi-Tenant Web Platform

### How It Works:
- Central web application (self-hosted or SaaS).
- Self-service onboarding portal:
    - GitHub integration (org/repo/token)
    - SonarQube integration (project key/token)
- Scheduled background job scans Sonar issues for onboarded repos.
- Metadata and issues stored in MongoDB.
- Admin dashboard shows issues, usage, progress.
- Button click or auto-triggered fix via LLM.
- PR generated with optional approval workflow.

### Benefits:
- Organization-wide scale (100K+ repos).
- Enables automation and governance.
- RBAC, audit trails, usage tracking, approval flows.
- Supports quotas and rate-limiting of LLM API usage.
- Central reporting and dashboards for leadership.
- Extendable via plugin architecture or API integrations.

### Drawbacks:
- Higher infrastructure and engineering investment.
- Requires DevOps and onboarding setup.
- Complex state management and multi-tenant logic.

---

## 5. ✅ Recommendation

| Use Case                      | Recommended Approach            |
|-------------------------------|---------------------------------|
| Developer Productivity        | IDE Plugin                      |
| Org-Wide Automation           | Scalable Platform               |
| Quick Adoption                | Plugin MVP                      |
| Long-Term Future-Proofing     | Central Backend + Plugin Access |

---

## 6. 📅 Proposed Roadmap

| Phase      | Timeline     | Milestone                                     |
|------------|--------------|-----------------------------------------------|
| Phase 1    | Week 1–2     | Finalize current POC and stabilize            |
| Phase 2    | Week 3–4     | Build IntelliJ/VSCode Plugin MVP             |
| Phase 3    | Week 5–6     | Create Onboarding APIs + GitHub OAuth        |
| Phase 4    | Week 7–10    | Launch scalable multi-tenant architecture     |
| Phase 5    | Week 11+     | Add RBAC, analytics, usage limits             |

---

## 7. 📝 Appendix

- All LLM prompts should follow a well-tested system prompt template that is configurable.
- Apply retry and fallback logic when LLM fails.
- Implement audit logging for every LLM request + file change.
- Plugin and Web App should both use a common backend service/API layer.
- Consider GitHub App model for enterprise tokenless integration.
- Apply governance through quota buckets per repo/team/user.
- Modularize services:
    - Git Service (clone/push/PR)
    - Sonar Service (scan issues)
    - Fix Service (LLM + Apply)
    - Scheduler/Batch Engine