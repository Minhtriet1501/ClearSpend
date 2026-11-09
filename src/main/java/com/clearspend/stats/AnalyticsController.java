package com.clearspend.stats;

import com.clearspend.anomaly.Flag;
import com.clearspend.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Aggregates powering the Statistics page: overview per government and agency deep-dive. */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class AnalyticsController {

    private final MongoTemplate mongo;

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(required = false) String source) {
        List<AggregationOperation> base = new ArrayList<>();
        if (source != null && !source.isBlank())
            base.add(ctx -> new Document("$match", new Document("source", source)));

        List<AggregationOperation> topOps = new ArrayList<>(base);
        topOps.add(ctx -> Document.parse("{ $group: { _id: '$agency', total: { $sum: '$amount' } } }"));
        topOps.add(ctx -> Document.parse("{ $sort: { total: -1 } }"));
        topOps.add(ctx -> Document.parse("{ $limit: 8 }"));
        List<Map<String, Object>> topAgencies = new ArrayList<>();
        for (Document d : agg(topOps))
            topAgencies.add(Map.of("agency", str(d.get("_id")), "total", dec(d.get("total"))));

        List<AggregationOperation> yearOps = new ArrayList<>(base);
        yearOps.add(ctx -> Document.parse("{ $match: { actionDate: { $ne: null } } }"));
        yearOps.add(ctx -> Document.parse("{ $group: { _id: { $year: '$actionDate' }, total: { $sum: '$amount' } } }"));
        yearOps.add(ctx -> Document.parse("{ $sort: { _id: 1 } }"));
        List<Map<String, Object>> byYear = new ArrayList<>();
        for (Document d : agg(yearOps))
            byYear.add(Map.of("year", d.get("_id"), "total", dec(d.get("total"))));

        long high = mongo.getCollection("flags").countDocuments(new Document("severity", "HIGH"));
        long medium = mongo.getCollection("flags").countDocuments(new Document("severity", "MEDIUM"));

        return Map.of("topAgencies", topAgencies, "byYear", byYear,
                "flags", Map.of("high", high, "medium", medium));
    }

    @GetMapping("/agency")
    public Map<String, Object> agency(@RequestParam String name) {
        Criteria c = Criteria.where("agency").is(name);
        long count = mongo.count(new Query(c), Transaction.class);

        List<AggregationOperation> totalOps = new ArrayList<>();
        totalOps.add(ctx -> new Document("$match", new Document("agency", name)));
        totalOps.add(ctx -> Document.parse("{ $group: { _id: null, total: { $sum: '$amount' } } }"));
        BigDecimal total = BigDecimal.ZERO;
        for (Document d : agg(totalOps)) total = dec(d.get("total"));

        List<AggregationOperation> recOps = new ArrayList<>();
        recOps.add(ctx -> new Document("$match", new Document("agency", name)));
        recOps.add(ctx -> Document.parse("{ $group: { _id: '$recipient', total: { $sum: '$amount' } } }"));
        recOps.add(ctx -> Document.parse("{ $sort: { total: -1 } }"));
        recOps.add(ctx -> Document.parse("{ $limit: 6 }"));
        List<Map<String, Object>> recipients = new ArrayList<>();
        for (Document d : agg(recOps)) {
            BigDecimal rt = dec(d.get("total"));
            double share = total.signum() > 0 ? rt.divide(total, 4, RoundingMode.HALF_UP).doubleValue() : 0;
            recipients.add(Map.of("recipient", str(d.get("_id")), "total", rt, "share", share));
        }

        List<AggregationOperation> yearOps = new ArrayList<>();
        yearOps.add(ctx -> new Document("$match", new Document("agency", name)));
        yearOps.add(ctx -> Document.parse("{ $match: { actionDate: { $ne: null } } }"));
        yearOps.add(ctx -> Document.parse("{ $group: { _id: { $year: '$actionDate' }, total: { $sum: '$amount' } } }"));
        yearOps.add(ctx -> Document.parse("{ $sort: { _id: 1 } }"));
        List<Map<String, Object>> byYear = new ArrayList<>();
        for (Document d : agg(yearOps))
            byYear.add(Map.of("year", d.get("_id"), "total", dec(d.get("total"))));

        Query idq = new Query(c);
        idq.fields().include("_id");
        List<String> ids = mongo.find(idq, Transaction.class).stream().map(Transaction::getId).toList();
        List<Flag> flags = ids.isEmpty() ? List.of()
                : mongo.find(new Query(Criteria.where("transactionId").in(ids)), Flag.class);

        return Map.of("agency", name, "total", total, "count", count,
                "topRecipients", recipients, "byYear", byYear, "flags", flags);
    }

    private List<Document> agg(List<AggregationOperation> ops) {
        return mongo.aggregate(Aggregation.newAggregation(ops), "transactions", Document.class).getMappedResults();
    }

    private static BigDecimal dec(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
