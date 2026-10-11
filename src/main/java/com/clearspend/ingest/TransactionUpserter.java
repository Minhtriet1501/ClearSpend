package com.clearspend.ingest;

import com.clearspend.transaction.Transaction;
import com.clearspend.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionUpserter {
    private final TransactionRepository repo;

    public void upsert(Transaction tx) {
        repo.findBySourceAndExternalId(tx.getSource(), tx.getExternalId())
                .ifPresent(existing -> tx.setId(existing.getId()));
        repo.save(tx);
    }
}
