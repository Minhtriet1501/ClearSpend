package com.clearspend.ingest;

import com.clearspend.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MesaConnector implements SourceConnector {
    private static final String SOURCE = "MesaAZ";

    private final RestClient socrataRestClient;
    private final TransactionUpserter upserter;

    @Value("${clearspend.mesa.base-url:https://citydata.mesaaz.gov}")
    private String baseUrl;
    @Value("${clearspend.mesa.dataset-id:j7s9-qiuq}")
    private String datasetId;

    @Override public String source() { return SOURCE; }

    @Override
    @SuppressWarnings("unchecked")
    public int ingestPage(int page, int limit) {
        int offset = (page - 1) * limit;
        String url = baseUrl + "/resource/" + datasetId + ".json"
                + "?$limit=" + limit + "&$offset=" + offset + "&$$exclude_system_fields=false";
        List<Map<String, Object>> rows = socrataRestClient.get()
                .uri(URI.create(url)).retrieve().body(List.class);
        if (rows == null) return 0;
        for (Map<String, Object> row : rows) upsert(row);
        return rows.size();

    }
    private void upsert(Map<String, Object> row) {
        String externalId = (String) row.get(":id");
        if (externalId == null) return;
        Transaction tx = Transaction.builder()
                .source(SOURCE)
                .externalId(externalId)
                .agency((String) row.get("department_name"))
                .recipient((String) row.get("vendor_name"))
                .amount(toBigDecimal(row.get("amount")))
                .actionDate(toLocalDate(row.get("payment_date")))
                .raw(new org.bson.Document(row))
                .ingestedAt(Instant.now())
                .build();
        upserter.upsert(tx);
    }
    private static BigDecimal toBigDecimal(Object v) {
        return v == null ? null : new BigDecimal(v.toString());
    }
    private static LocalDate toLocalDate(Object v) {
        return v == null ? null : LocalDate.parse(v.toString().substring(0, 10)); // "2015-07-01T..." → "2015-07-01"
    }
}

