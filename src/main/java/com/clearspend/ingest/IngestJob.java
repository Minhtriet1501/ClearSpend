package com.clearspend.ingest;

public record IngestJob(String source, int page, int limit) {}
