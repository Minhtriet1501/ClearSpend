package com.clearspend.transaction;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import java.util.regex.Pattern;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSearchTest {

    @Test
    void noFilters_buildsEmptyQuery() {
        Query q = TransactionController.buildSearchQuery(null, null, null);
        assertTrue(q.getQueryObject().isEmpty(), "không filter thì query rỗng = trả tất cả");
    }

    @Test
    void blankStrings_areIgnored() {
        Query q = TransactionController.buildSearchQuery("  ", "", null);
        assertTrue(q.getQueryObject().isEmpty(), "chuỗi trống/space không tạo criteria");
    }

    @Test
    void agencyAndMinAmount_combineUnderAnd() {
        Query q = TransactionController.buildSearchQuery("Energy", null, new BigDecimal("1000"));
        Document filter = q.getQueryObject();

        @SuppressWarnings("unchecked")
        List<Document> and = (List<Document>) filter.get("$and");
        assertNotNull(and, "nhiều điều kiện phải gộp bằng $and");
        assertEquals(2, and.size());

        // agency: Spring Data lưu regex dạng java.util.regex.Pattern (không phải Document $regex)
        Pattern agencyClause = (Pattern) and.get(0).get("agency");
        assertTrue(agencyClause.pattern().contains("Energy"));
        assertTrue((agencyClause.flags() & Pattern.CASE_INSENSITIVE) != 0, "phải case-insensitive");

        // amount >= 1000
        Document amountClause = and.get(1).get("amount", Document.class);
        assertEquals(0, new BigDecimal(amountClause.get("$gte").toString())
                .compareTo(new BigDecimal("1000")));
    }
}
