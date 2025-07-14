package com.org.devgenie.service;

import com.org.devgenie.model.CoverageComponentNode;
import com.org.devgenie.mongo.CoverageComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CoverageTreeService {
    private static final Logger logger = LoggerFactory.getLogger(CoverageTreeService.class);

    @Value("${sonar.url}") private String sonarUrl;
    @Value("${sonar.username}") private String sonarUsername;
    @Value("${sonar.password}") private String sonarPassword;
    @Autowired
    private RestTemplate restTemplate;
    private CoverageComponentRepository coverageComponentRepository;

    private final CoverageComponentRepository repository;

    @Autowired
    public CoverageTreeService(CoverageComponentRepository repository) {
        this.repository = repository;
    }

    public List<CoverageComponentNode> fetchAndStoreComponentsWithCoverage(String projectKey) {
        long startTime = System.nanoTime();
        logger.info("Started fetching coverage tree for project: {}", projectKey);

        List<CoverageComponentNode> nodes = fetchCoverageNodes(projectKey);

        // Filter only src/main components
        List<CoverageComponentNode> mainNodes = nodes.stream()
                .filter(node -> node.getPath() != null && node.getPath().startsWith("src/main"))
                .collect(Collectors.toList());

        repository.saveAll(mainNodes);

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        logger.info("Coverage tree fetched and stored for project: {} in {} ms", projectKey, duration);
        return mainNodes;
    }

    private HttpEntity getAuthHeaders(){
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBasicAuth(sonarUsername, sonarPassword);
        headers.setCacheControl(CacheControl.noCache());
        return new HttpEntity<>(headers);
    }

    private List<CoverageComponentNode> fetchCoverageNodes(String projectKey) {
        logger.info("Starting fetchCoverageNodes for project: {}", projectKey);
        int pageIndex = 1;
        int pageSize = 50;
        List<CoverageComponentNode> allNodes = new ArrayList<>();
        int totalPages = Integer.MAX_VALUE;

        try {
            while (pageIndex <= totalPages) {
                String uri = String.format(
                        "%smeasures/component_tree?component=%s&metricKeys=coverage,line_coverage,branch_coverage,lines_to_cover,uncovered_lines,conditions_to_cover,uncovered_conditions&ps=%d&p=%d",
                        sonarUrl, projectKey, pageSize, pageIndex
                );
                logger.debug("Requesting SonarQube API: {}", uri);

                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, getAuthHeaders(), Map.class);
                Map<String, Object> body = response.getBody();

                if (body == null) {
                    logger.warn("No response body for page {} of project {}", pageIndex, projectKey);
                    break;
                }

                List<Map<String, Object>> components = (List<Map<String, Object>>) body.getOrDefault("components", Collections.emptyList());
                for (Map<String, Object> comp : components) {
                    String key = (String) comp.get("key");
                    String path = (String) comp.get("path");
                    String type = (String) comp.get("qualifier");
                    List<Map<String, String>> measures = (List<Map<String, String>>) comp.getOrDefault("measures", Collections.emptyList());

                    CoverageComponentNode node = new CoverageComponentNode();
                    node.setKey(key);
                    node.setPath(path);
                    node.setType(type);

                    Map<String, Double> metricsMap = new HashMap<>();
                    for (Map<String, String> measure : measures) {
                        String metric = measure.get("metric");
                        String value = measure.get("value");
                        if (metric != null && value != null) {
                            try {
                                metricsMap.put(metric, Double.parseDouble(value));
                            } catch (NumberFormatException e) {
                                logger.error("Metric parse error for key {} metric {}: {}", key, metric, e.getMessage());
                            }
                        }
                    }
                    node.setMetricsMap(metricsMap);
                    allNodes.add(node);
                }

                if (pageIndex == 1 && body.containsKey("paging")) {
                    Map<String, Object> paging = (Map<String, Object>) body.get("paging");
                    int total = ((Number) paging.get("total")).intValue();
                    totalPages = (int) Math.ceil((double) total / pageSize);
                    logger.info("Total components: {}, total pages: {}", total, totalPages);
                }

                if (components.isEmpty()) {
                    logger.info("No more components found at page {} for project {}", pageIndex, projectKey);
                    break;
                }
                pageIndex++;
            }
        } catch (Exception ex) {
            logger.error("Error fetching coverage nodes for project {}: {}", projectKey, ex.getMessage(), ex);
        }

        logger.info("Completed fetchCoverageNodes for project: {}. Total nodes fetched: {}", projectKey, allNodes.size());
        return allNodes;
    }

    public List<CoverageComponentNode> getAllComponents() {
        return repository.findAll();
    }


}