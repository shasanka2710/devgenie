# 📦 VS Code Plugin for Auto-Fixing SonarLint Issues using Copilot

## 🧩 Logical Grouping of Requirements

---

## 1. 🔧 Plugin Initialization & Configuration

| Feature | Description |
|--------|-------------|
| **Activation Events** | Plugin activates on VS Code start or command. |
| **Configuration Settings** | - Set Copilot/LLM Token<br>- Customize system prompt<br>- Enable/disable auto-fix<br>- Supported languages configuration |
| **Sidebar Registration** | Registers the SonarQube sidebar panel upon activation. |

---

## 2. 🔍 Issue Detection & Retrieval

| Feature | Description |
|--------|-------------|
| **SonarLint Integration** | Leverage SonarLint extension API or run CLI to scan the open workspace. |
| **Issue Metadata Parsing** | Extract severity, file name, line number, and issue description. |
| **Local Caching (Optional)** | Cache issue data locally to reduce reprocessing time. |

---

## 3. 📊 UI Display & Interaction

| Feature | Description |
|--------|-------------|
| **Sidebar UI with Severity Groups** | Group issues under collapsible panels: Blocker, Critical, Major, Minor, Info. |
| **Tabular Display** | Each severity group shows:<br>✔️ Checkbox<br>📄 Issue Description<br>📁 File Name<br>🛠 Apply Fix button |
| **Bulk Selection and Fixing** | Multi-select checkboxes + a group-level “Apply Fix” button for bulk LLM execution. |
| **Refresh Button** | Triggers re-scan of current workspace. |

---

## 4. 🤖 LLM (Copilot) Integration

| Feature | Description |
|--------|-------------|
| **Prompt Construction** | System prompt that includes issue description, file path, and raw source code. |
| **Copilot API Request** | Send prompt and receive a fixed version of the code. |
| **Timeouts & Retries** | Set limits on LLM wait times, retry upon transient failure. |
| **Streaming Response Support (Optional)** | For long outputs, stream partial content as it's generated. |

---

## 5. 📝 Code Application Engine

| Feature | Description |
|--------|-------------|
| **Code Replace Logic** | Overwrites selected portion or entire file with the updated code. |
| **Cursor & Scroll Preservation** | Retain editor position after applying fix. |
| **Optional Diff Preview** | Show a visual diff before confirming overwrite. |
| **Undo Support** | Allow user to revert the last applied fix using VS Code undo stack. |

---

## 6. ✅ Bulk Fix Handling

| Feature | Description |
|--------|-------------|
| **Batch Fix Trigger** | Collect multiple selected issues and apply fixes sequentially. |
| **Progress Bar or Spinner** | UI element to show ongoing batch fix operation. |
| **Per-Issue Result Feedback** | Success/failure alert for each individual issue in the batch. |

---

## 7. 📡 Feedback & Notifications

| Feature | Description |
|--------|-------------|
| **Toast/Alerts** | Display success or error messages after applying fixes. |
| **Status Bar Integration** | Show background operation status or ongoing task summary. |
| **Logs for Debugging** | Provide logs in the developer console for failed LLM calls or application issues. |

---

## 8. 🧪 Testing & Reliability Features

| Feature | Description |
|--------|-------------|
| **Dry Run Mode** | Preview changes without applying them. |
| **Validation Before Apply** | Check Copilot output for malformed or empty responses. |
| **Telemetry (Optional)** | Collect anonymized usage metrics to understand adoption. |

---

## 9. 🔒 Security & Permissions

| Feature | Description |
|--------|-------------|
| **Token Security** | Store Copilot/LLM tokens securely via VS Code secrets API. |
| **Permission Prompts** | Ask before modifying files. |
| **Scope Restriction** | Only process files within workspace root folder. |

---