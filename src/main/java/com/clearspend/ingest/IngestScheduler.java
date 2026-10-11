package com.clearspend.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestScheduler {
    private final SqsClient sqs;
    private final IngestRunRepository runRepo;
    private final ObjectMapper objectMapper;

    @Value("${clearspend.sqs.ingest-queue-url}")
    private String queueUrl;

    public int enqueue(String source, int pages, int limit) {
        for(int page = 1; page <= pages; page++) {
            String body = objectMapper.writeValueAsString(new IngestJob(source, page, limit));
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        }
        log.info("Enqueued {} page-jobs for source={}", pages, source);
        runRepo.save(IngestRun.builder()
                .source(source).pages(pages).limit(limit)
                .jobsEnqueued(pages).startedAt(Instant.now())
                .build());
        return pages;
    }
}
