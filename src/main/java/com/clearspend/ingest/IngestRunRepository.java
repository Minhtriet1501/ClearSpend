package com.clearspend.ingest;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IngestRunRepository extends MongoRepository<IngestRun, String> {
    List<IngestRun> findTop20ByOrderByStartedAtDesc();
}
