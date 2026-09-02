package com.clearspend.ingest;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IngestController {
    private final UsaSpendingConnector connector;

    @PostMapping("/internal/ingest/usaspending")
    public Map<String, Object> ingest(
            @RequestParam(defaultValue = "2") int pages,
            @RequestParam(defaultValue = "100") int limit) {
        int n = connector.ingest(pages, limit);
        return Map.of("source", "USAspending", "processed", n);
    }

}
