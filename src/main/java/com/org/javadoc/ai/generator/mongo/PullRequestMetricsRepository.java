package com.org.javadoc.ai.generator.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PullRequestMetricsRepository extends MongoRepository<PullRequestMetrics, String> {
}