package com.clearspend.ingest;

import com.clearspend.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class UsaSpendingConnector implements SourceConnector{


    private static final String SOURCE = "USAspending";
    private static final List<String> FIELDS = List.of(
            "Award ID", "Recipient Name", "Awarding Agency", "Award Amount", "Start Date");
    private final TransactionUpserter upserter;

    private final RestClient usaSpendingClient;


    @Override public String source() { return SOURCE; }

    @Override
    public int ingestPage(int page, int limit) {
        SpendingByAwardResponse resp = fetchPage(page, limit);
        if (resp == null || resp.results() == null) return 0;
        int n = 0;
        for(java.util.Map<String, Object> row : resp.results()) {
            upsert(row);
            n++;
        }
        return n;
    }

    private SpendingByAwardResponse fetchPage(int page, int limit){
        Map<String, Object> body = Map.of(
                "filters", Map.of(
                        "time_period", List.of(Map.of("start_date", "2023-01-01", "end_date", "2024-12-31")),
                        "award_type_codes", List.of("A", "B", "C", "D")),
                "fields", FIELDS,
                "page", page,
                "limit", limit,
                "sort", "Award Amount",
                "order", "desc"
        );

        return usaSpendingClient.post()
                .uri("/api/v2/search/spending_by_award/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(SpendingByAwardResponse.class);
    }

    private void upsert(Map<String, Object> row){
        String externalId = (String) row.get("generated_internal_id");
        if (externalId == null) return;

        Transaction tx = Transaction.builder()
                .source(SOURCE)
                .externalId(externalId)
                .agency((String) row.get("Awarding Agency"))
                .recipient((String) row.get("Recipient Name"))
                .amount(toBigDecimal(row.get("Award Amount")))
                .actionDate(toLocalDate(row.get("Start Date")))
                .raw(new org.bson.Document(row))
                .ingestedAt(Instant.now())
                .build();

        upserter.upsert(tx);
    }

    private BigDecimal toBigDecimal(Object v){
        return v == null ? null : new BigDecimal(v.toString());
    }

    private LocalDate toLocalDate(Object v){
        return v == null ? null : LocalDate.parse(v.toString());
    }
}
