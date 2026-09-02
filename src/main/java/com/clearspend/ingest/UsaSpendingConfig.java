package com.clearspend.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UsaSpendingConfig {

    @Bean
    RestClient usaSpendingClient(
            @Value("${usaspending.base-url:https://api.usaspending.gov}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
