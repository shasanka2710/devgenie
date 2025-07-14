package com.org.devgenie.model.coverage;

@Component
@Slf4j
public class CoverageReportGenerator {

    public String generateHtmlReport(CoverageData coverageData, List<FileChange> changes) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Coverage Report</title>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;}");
        html.append(".metric{background:#f5f5f5;padding:10px;margin:10px 0;border-radius:5px;}");
        html.append(".file{margin:10px 0;padding:10px;border:1px solid #ddd;}");
        html.append("</style></head><body>");

        html.append("<h1>Code Coverage Report</h1>");
        html.append("<div class='metric'>");
        html.append("<h2>Overall Metrics</h2>");
        html.append(String.format("<p>Line Coverage: %.2f%%</p>", coverageData.getLineCoverage()));
        html.append(String.format("<p>Branch Coverage: %.2f%%</p>", coverageData.getBranchCoverage()));
        html.append(String.format("<p>Method Coverage: %.2f%%</p>", coverageData.getMethodCoverage()));
        html.append("</div>");

        html.append("<h2>Files Processed</h2>");
        for (FileChange change : changes) {
            html.append("<div class='file'>");
            html.append(String.format("<h3>%s</h3>", change.getFilePath()));
            html.append(String.format("<p>Test File: %s</p>", change.getTestFilePath()));
            html.append(String.format("<p>Change Type: %s</p>", change.getChangeType()));
            html.append(String.format("<p>Description: %s</p>", change.getDescription()));
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}

