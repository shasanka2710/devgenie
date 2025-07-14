package com.org.devgenie.service.coverage;

@Service
@Slf4j
public class CoverageDataService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JacocoService jacocoService;

    @Value("${coverage.data.use-mongo:true}")
    private boolean useMongoData;

    public CoverageData getCurrentCoverage(String repoPath) {
        log.info("Getting current coverage data for repo: {}", repoPath);

        if (useMongoData) {
            try {
                return getCoverageFromMongo(repoPath);
            } catch (Exception e) {
                log.warn("Failed to get coverage from MongoDB, falling back to Jacoco", e);
                return jacocoService.runAnalysis(repoPath);
            }
        } else {
            return jacocoService.runAnalysis(repoPath);
        }
    }

    public FileCoverageData getFileCoverage(String filePath) {
        Query query = new Query(Criteria.where("filePath").is(filePath));
        FileCoverageData data = mongoTemplate.findOne(query, FileCoverageData.class, "file_coverage");

        if (data == null) {
            log.warn("No coverage data found for file: {}, creating default", filePath);
            return createDefaultFileCoverage(filePath);
        }

        return data;
    }

    public void saveCoverageData(CoverageData coverageData) {
        try {
            mongoTemplate.save(coverageData, "repo_coverage");

            // Save individual file data
            for (FileCoverageData fileData : coverageData.getFiles()) {
                mongoTemplate.save(fileData, "file_coverage");
            }

            log.info("Saved coverage data for {} files", coverageData.getFiles().size());
        } catch (Exception e) {
            log.error("Failed to save coverage data", e);
        }
    }

    private CoverageData getCoverageFromMongo(String repoPath) {
        Query query = new Query(Criteria.where("repoPath").is(repoPath));
        query.with(Sort.by(Sort.Direction.DESC, "timestamp")).limit(1);

        CoverageData data = mongoTemplate.findOne(query, CoverageData.class, "repo_coverage");

        if (data == null) {
            throw new CoverageDataNotFoundException("No coverage data found for repo: " + repoPath);
        }

        return data;
    }

    private FileCoverageData createDefaultFileCoverage(String filePath) {
        return FileCoverageData.builder()
                .filePath(filePath)
                .lineCoverage(0.0)
                .branchCoverage(0.0)
                .methodCoverage(0.0)
                .uncoveredLines(new ArrayList<>())
                .uncoveredBranches(new ArrayList<>())
                .totalLines(0)
                .totalBranches(0)
                .totalMethods(0)
                .build();
    }
}
