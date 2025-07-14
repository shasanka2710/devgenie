package com.org.devgenie.mongo;

import com.org.devgenie.model.CoverageComponentNode;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CoverageComponentRepository extends MongoRepository<CoverageComponentNode, String> {
}
