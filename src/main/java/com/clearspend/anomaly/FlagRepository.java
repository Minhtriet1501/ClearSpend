package com.clearspend.anomaly;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FlagRepository extends MongoRepository<Flag, String> {
    Optional<Flag> findByTransactionIdAndRule(String transactionId, String rule);
    List<Flag> findTop50ByOrderByCreatedAtDesc();
}
