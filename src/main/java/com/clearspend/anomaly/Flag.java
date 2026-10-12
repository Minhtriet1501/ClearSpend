package com.clearspend.anomaly;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data @Builder @NoArgsConstructor
@AllArgsConstructor @Document(collection = "flags")
@CompoundIndex(name = "tx_rule_unique", def = "{'transactionId': 1, 'rule': 1}", unique = true)
public class Flag {
    @Id
    private String id;
    private String transactionId;   // _id of flag transaction
    private String rule;            // "SPIKE_VS_MEDIAN", "VENDOR_CONCENTRATION"
    private String severity;        // "MEDIUM" / "HIGH"
    private String explanation;     // human language
    private Instant createdAt;
}
