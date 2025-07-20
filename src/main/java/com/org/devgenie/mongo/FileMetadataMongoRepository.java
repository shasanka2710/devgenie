package com.org.devgenie.mongo;

import com.org.devgenie.service.metadata.MetadataAnalyzer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataMongoRepository extends MongoRepository<MetadataAnalyzer.FileMetadata, String> {
    public List<MetadataAnalyzer.FileMetadata> findByRepositoryUrlAndBranch(String repositoryUrl, String branch);
}
