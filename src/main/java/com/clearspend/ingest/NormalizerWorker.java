package com.clearspend.ingest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NormalizerWorker {

    private final SqsClient sqs;
    private final ObjectMapper objectMapper;

    private final List<SourceConnector> connectorList;
    private Map<String, SourceConnector> connectors;

    @Value("${clearspend.sqs.ingest-queue-url}")
    private String queueUrl;

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    @PostConstruct
    void start() {
        connectors = connectorList.stream()
                .collect(java.util.stream.Collectors.toMap(SourceConnector::source, c -> c));
        pool.submit(this::loop);
        log.info("NormalizerWorker started");
    }

    private void loop() {
        while (running) {
            try {
                List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20)       // long polling: chờ tới 20s, có message là trả ngay
                        .build()).messages();
                for (Message m : messages) handle(m);
            } catch (Exception e) {
                log.error("Worker loop error", e);
            }
        }
    }

    private void handle(Message m) {
        try {
            IngestJob job = objectMapper.readValue(m.body(), IngestJob.class);
            SourceConnector connector = connectors.get(job.source());
            if (connector == null) {   // source lạ → xoá message cho khỏi kẹt (hoặc để DLQ tuỳ bạn)
                log.error("Unknown source: {}", job.source());
                sqs.deleteMessage(b -> b.queueUrl(queueUrl).receiptHandle(m.receiptHandle()));
                return;
            }
            int n = connector.ingestPage(job.page(), job.limit());
            log.info("Processed page {} -> {} records", job.page(), n);
            sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
        } catch (Exception e) {
            log.error("Job failed, để SQS retry/DLQ: {}", m.body(), e);
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        pool.shutdown();
    }
}
