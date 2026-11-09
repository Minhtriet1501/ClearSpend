package com.clearspend.transaction;

import com.clearspend.anomaly.Flag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    /** Distinct agency names, for the filter dropdown. */
    @GetMapping("/agencies")
    public List<String> agencies() {
        return mongo.findDistinct(new Query(), "agency", "transactions", String.class)
                .stream().filter(a -> a != null && !a.isBlank()).sorted().toList();
    }

    @GetMapping("/search")
    public Page<Transaction> search(
            @RequestParam(required = false) String agency,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String severity,
            Pageable pageable) {

        Query query = buildSearchQuery(agency, recipient, minAmount, maxAmount, source, dateFrom, dateTo);

        // "flagged" / "high" filter spans the flags collection: resolve matching transaction ids
        if (severity != null && !severity.isBlank()) {
            query.addCriteria(Criteria.where("id").in(flaggedTxIds(severity)));
        }

        long total = mongo.count(query, Transaction.class);
        List<Transaction> content = mongo.find(query.with(pageable), Transaction.class);
        return new PageImpl<>(content, pageable, total);
    }

    /** Transaction ids that carry a flag; when severity = HIGH, only high-severity flags. */
    private List<String> flaggedTxIds(String severity) {
        Query q = new Query();
        if ("HIGH".equalsIgnoreCase(severity)) {
            q.addCriteria(Criteria.where("severity").is("HIGH"));
        }
        q.fields().include("transactionId");
        return mongo.find(q, Flag.class).stream()
                .map(Flag::getTransactionId)
                .distinct()
                .toList();
    }

    /** Overload kept for the unit test and simple callers. */
    static Query buildSearchQuery(String agency, String recipient, BigDecimal minAmount) {
        return buildSearchQuery(agency, recipient, minAmount, null, null, null, null);
    }

    static Query buildSearchQuery(String agency, String recipient, BigDecimal minAmount,
                                  BigDecimal maxAmount, String source, LocalDate dateFrom, LocalDate dateTo) {
        var criteria = new ArrayList<Criteria>();
        if (agency != null && !agency.isBlank())
            criteria.add(Criteria.where("agency").regex(Pattern.quote(agency), "i"));
        if (recipient != null && !recipient.isBlank())
            criteria.add(Criteria.where("recipient").regex(Pattern.quote(recipient), "i"));
        if (minAmount != null)
            criteria.add(Criteria.where("amount").gte(minAmount));
        if (maxAmount != null)
            criteria.add(Criteria.where("amount").lte(maxAmount));
        if (source != null && !source.isBlank())
            criteria.add(Criteria.where("source").is(source));
        if (dateFrom != null)
            criteria.add(Criteria.where("actionDate").gte(dateFrom));
        if (dateTo != null)
            criteria.add(Criteria.where("actionDate").lte(dateTo));

        Query query = new Query();
        if (!criteria.isEmpty())
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        return query;
    }
}
