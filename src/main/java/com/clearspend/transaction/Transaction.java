package com.clearspend.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
@CompoundIndex(name = "src_ext_unique", def = "{'source': 1, 'externalId': 1}",  unique = true)
public class Transaction {
    @Id
    private String id;

    private String source;
    private String externalId;

    private String agency;
    private String recipient;
    private BigDecimal amount; //no double. Money need to be exactly
    private LocalDate actionDate;

    //full path because it overlaps the spring Document
    private org.bson.Document raw;

    private Instant ingestedAt;
}
