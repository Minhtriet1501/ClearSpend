package com.clearspend.ingest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record SpendingByAwardResponse(List<Map<String, Object>> results,
                                      PageMetadata page_metadata){
    public record PageMetadata(int page, boolean hasNext){}
}
