package com.clearspend.ingest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "ingest_runs")
public class IngestRun {
    @Id private String id;
    private String source;
    private int pages;
    private int limit;
    private int jobsEnqueued;
    private Instant startedAt;
}
