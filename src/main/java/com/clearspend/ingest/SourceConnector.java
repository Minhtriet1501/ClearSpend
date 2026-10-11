package com.clearspend.ingest;

public interface SourceConnector {
    String source ();
    int ingestPage (int page, int limit);

}
