package com.clearspend.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {

    @Bean
    SqsClient sqsClient(@Value("${aws.region:us-west-2}") String region) {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
    @Bean
    RestClient socrataRestClient() {
        return RestClient.create();   // không base-url; connector truyền URI tuyệt đối
    }
}
