package com.clearspend.anomaly;

import com.clearspend.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyService {
    private final MongoTemplate mongo;
    private final FlagRepository flagRepo;

    @Value("${clearspend.anomaly.spike-k:8}")
    private double k;

    @Value("${clearspend.anomaly.vendor-share:0.5}")
    private double vendorShare;   // cờ nếu 1 NT > 50% tổng chi cơ quan

    public int detectSpikes() {
        Aggregation agg = Aggregation.newAggregation(ctx -> Document.parse(
                "{ $group: { _id: '$agency', median: { $median: { input: '$amount', method: 'approximate' } } } }")
        );
        AggregationResults<Document> res = mongo.aggregate(agg, "transactions", Document.class);

        int flagged = 0;
        for(Document row : res) {
            String agency = row.getString("_id");
            if(agency == null || row.get("median") == null) continue;
            BigDecimal median = new  BigDecimal(row.get("median").toString());
            if(median.signum() <= 0 ) continue;
            BigDecimal threshold = median.multiply(BigDecimal.valueOf(k));

            List<Transaction> spikes = mongo.find(
                    new Query(Criteria.where("agency").is(agency).and("amount").gt(threshold)),
                    Transaction.class);

            for(Transaction t : spikes) {
                double ratio = t.getAmount().divide(median, 1, RoundingMode.HALF_UP).doubleValue();
                String explanation = String.format(
                        "%s paid $%,.0f to %s — %.1f× the %s median of $%,.0f.",
                        agency, t.getAmount(), t.getRecipient(), ratio, agency, median);

                upsertFlag(t.getId(), "SPIKE_VS_MEDIAN", ratio >= 20 ? "HIGH" : "MEDIUM", explanation);

                flagged++;
            }
        }
        log.info("Rule SPIKE_VS_MEDIAN: {} flags", flagged);
        return flagged;
    }

    @SuppressWarnings("unchecked")
    public int detectVendorConcentration() {
        List<AggregationOperation> ops = List.of(ctx -> org.bson.Document.parse(
                        "{ $group: { _id: { agency: '$agency', recipient: '$recipient' }, spent: { $sum: '$amount' } } }"),
                ctx -> org.bson.Document.parse(
                        "{ $group: { _id: '$_id.agency', total: { $sum: '$spent' }, " +
                                "vendors: { $push: { recipient: '$_id.recipient', spent: '$spent' } } } }"));

        AggregationResults<org.bson.Document> res =
                mongo.aggregate(Aggregation.newAggregation(ops), "transactions", org.bson.Document.class);
        int flagged = 0;
        for(Document agencyDoc : res) {
            String agency = agencyDoc.getString("_id");
            if (agency == null || agencyDoc.get("total") == null) continue;
            BigDecimal total = new BigDecimal(agencyDoc.get("total").toString());
            if(total.signum() <= 0 ) continue;

            List<Document> vendors =  (List<Document>) agencyDoc.get("vendors");
            if (vendors == null || vendors.size() < 2) continue;

            for(Document v : vendors) {
                String recipient = v.getString("recipient");
                if(recipient == null || v.get("spent") == null) continue;
                BigDecimal spent = new BigDecimal(v.get("spent").toString());
                double share = spent.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
                if(share < vendorShare) continue;

                Transaction rep = mongo.findOne(
                        new Query(Criteria.where("agency").is(agency).and("recipient").is(recipient))
                                .with(Sort.by(Sort.Direction.DESC, "amount")),
                        Transaction.class);

                if (rep == null) continue;
                String explanation = String.format(
                        "%s received %.0f%% of %s's spend ($%,.0f of $%,.0f).",
                        recipient, share * 100, agency, spent, total);
                upsertFlag(rep.getId(), "VENDOR_CONCENTRATION", share >= 0.8 ? "HIGH" : "MEDIUM", explanation);
                flagged++;
            }
        }
        log.info("Rule VENDOR_CONCENTRATION: {} flags", flagged);
        return flagged;
    }

    private void upsertFlag(String transactionId, String rule, String severity, String explanation) {
        Flag flag = Flag.builder()
                .transactionId(transactionId).rule(rule).severity(severity)
                .explanation(explanation).createdAt(Instant.now()).build();
        flagRepo.findByTransactionIdAndRule(transactionId, rule)
                .ifPresent(existing -> flag.setId(existing.getId()));
        flagRepo.save(flag);
    }


}
