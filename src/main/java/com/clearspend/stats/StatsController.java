package com.clearspend.stats;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final MongoTemplate mongo;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long transactions = mongo.getCollection("transactions").estimatedDocumentCount();
        long flags = mongo.getCollection("flags").countDocuments();
        long flagsHigh = mongo.getCollection("flags").countDocuments(new Document("severity", "HIGH"));

        Aggregation agg = Aggregation.newAggregation(ctx -> Document.parse(
                "{ $group: { _id: null, total: { $sum: '$amount' } } }"));
        AggregationResults<Document> res = mongo.aggregate(agg, "transactions", Document.class);
        Document row = res.getUniqueMappedResult();
        BigDecimal totalValue = (row == null || row.get("total") == null)
                ? BigDecimal.ZERO : new BigDecimal(row.get("total").toString());

        return Map.of("transactions", transactions, "totalValue", totalValue,
                "flags", flags, "flagsHigh", flagsHigh);
    }
}