# DevGenie 🪄 – AI-Powered SonarQube Issue Resolver

**DevGenie** is an AI-powered utility designed to automate the resolution of SonarQube issues and streamline the software development lifecycle. With its intuitive web interface and automated workflow, DevGenie empowers developers to address code issues efficiently, reducing technical debt and accelerating delivery.

---

## 🚀 Features
- 📊 **Dashboard Reporting:** View real-time reports of resolved SonarQube issues and auto-created GitHub PRs.
- 💡 **AI-Powered Fixes:** Automatically generate and validate code fixes using Google Gemini Vertex AI.
- 🛠️ **One-Click Issue Resolution:** Select issues directly from the Insights screen and let DevGenie handle the rest.
- 🗃️ **Automated PR Creation:** Generate pull requests with AI-driven fixes and commit them to GitHub.
- 🧠 **Context-Aware Fixes:** Utilizes JavaParser to deeply understand code structure for accurate fixes.

---

## 🛠️ Tech Stack
- **Backend:** Spring Boot, Spring AI, JavaParser
- **Frontend:** Spring Thymeleaf, Bootstrap, JavaScript
- **AI Model:** Google Gemini Vertex 1.5 Pro via Spring AI
- **Version Control:** GitHub (Automated PRs)
- **Code Analysis:** SonarQube
- **Build Tool:** Gradle

---

## 🛤️ Workflow Overview
1. **Scan Issues:** Fetches issues from SonarQube.
2. **Gather Context:** Analyzes code structure using JavaParser.
3. **Generate Fix:** Interacts with Google Gemini Vertex AI (via Spring AI) to suggest fixes.
4. **Validate Solution:** Reviews and tests the generated solution.
5. **Create PR:** Automatically pushes fixes and creates a GitHub pull request.

---

## 📊 System Interaction Diagram
[Placeholder: Insert your system architecture diagram here]

---

## 💻 Web Interface
### **1. Home Screen**
- Displays a **Dashboard Report** showcasing:
    - ✅ Number of resolved SonarQube issues
    - 📝 List of auto-created GitHub PRs
- **Bug Issues Section:** Displays automatically resolved issues with links to their PRs.

### **2. Insights Screen**
- 📌 Lists all SonarQube issues.
- 🛎️ Allows developers to resolve issues individually or in bulk with a **"Fix It"** button.
- ⚙️ Provides real-time status of issue resolutions.

---

## 🧩 Installation
### **Prerequisites:**
- Java 17+
- Gradle
- Docker (for SonarQube)
- **Access Tokens:** SonarQube, GitHub, and Google Gemini API

### **Clone the Repository:**
```bash
git clone https://github.com/yourusername/devgenie.git  
cd devgenie  
```

### **Set Up Environment Variables:**
Create a `.env` file in the project root and add:
```
SONARQUBE_URL=http://localhost:9000  
SONARQUBE_TOKEN=your_sonarqube_token  
GITHUB_TOKEN=your_github_token  
GOOGLE_API_KEY=your_google_api_key  
```

### **Run the Application:**
```bash
gradle clean build  
gradle bootRun  
```

---

## 🧪 Usage
1. **Access the UI:** Open your browser and visit `http://localhost:8080`
2. **View Dashboard:** Explore the resolved issues and PR reports.
3. **Resolve Issues:** Go to the *Insights* screen and click **Fix It** to resolve issues via AI.
4. **Review PRs:** Navigate to your GitHub repository to review and merge the auto-created pull requests.

---

## 💡 Contributing
We welcome contributions! Please fork the repository and submit pull requests.

---

## 🛡️ License
This project is licensed under the MIT License.

