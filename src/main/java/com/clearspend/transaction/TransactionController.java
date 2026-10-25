package com.clearspend.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository repo;
    private final MongoTemplate mongo;

    @GetMapping
    public Page<Transaction> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @GetMapping("/search")
    public Page<Transaction> search(
            @RequestParam(required = false) String agency,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) BigDecimal minAmount,
            Pageable pageable) {

        Query query = buildSearchQuery(agency, recipient, minAmount);
        long total = mongo.count(query, Transaction.class);
        var content = mongo.find(query.with(pageable), Transaction.class);
        return new PageImpl<>(content, pageable, total);
    }


    static Query buildSearchQuery(String agency, String recipient, BigDecimal minAmount) {
        var criteria = new ArrayList<Criteria>();
        if (agency != null && !agency.isBlank())
            criteria.add(Criteria.where("agency").regex(Pattern.quote(agency), "i"));
        if (recipient != null && !recipient.isBlank())
            criteria.add(Criteria.where("recipient").regex(Pattern.quote(recipient), "i"));
        if (minAmount != null)
            criteria.add(Criteria.where("amount").gte(minAmount));

        Query query = new Query();
        if (!criteria.isEmpty())
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        return query;
    }
}
