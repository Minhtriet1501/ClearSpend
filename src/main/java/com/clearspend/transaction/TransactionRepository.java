package com.clearspend.transaction;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface TransactionRepository extends MongoRepository<Transaction, String> {
    //upsert idempotent
    Optional<Transaction> findBySourceAndExternalId(String source, String externalId);
}

