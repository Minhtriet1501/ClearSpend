package com.clearspend.ingest;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IngestController {
    private final IngestScheduler scheduler;
    private final IngestRunRepository runRepo;

    @PostMapping("/internal/ingest/{source}")
    public Map<String, Object> ingest(
            @PathVariable String source,
            @RequestParam(defaultValue = "2") int pages,
            @RequestParam(defaultValue = "100") int limit) {
        int jobs = scheduler.enqueue(source, pages, limit);
        return Map.of("source", source, "enqueued", jobs);
    }

    @GetMapping("/internal/ingest/runs")
    public java.util.List<IngestRun> runs() {
        return runRepo.findTop20ByOrderByStartedAtDesc();
    }

}
