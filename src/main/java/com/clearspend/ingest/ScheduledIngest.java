package com.clearspend.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "clearspend.ingest.scheduled", havingValue = "true")
public class ScheduledIngest {

    private final IngestScheduler scheduler;

    @Scheduled(fixedDelayString = "${clearspend.ingest.schedule-ms:3600000}",
                initialDelayString = "${clearspend.ingest.initial-delay-ms:10000}")
    public void run() {
        log.info("Scheduled ingest tick");
        scheduler.enqueue("USAspending", 2, 100);
        scheduler.enqueue("MesaAZ", 2, 100);
    }
}
