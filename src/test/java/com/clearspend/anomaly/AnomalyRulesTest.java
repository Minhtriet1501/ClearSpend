package com.clearspend.anomaly;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AnomalyRulesTest {

    @Test
    void spikeThreshold_isMedianTimesK() {
        assertEquals(0, AnomalyRules.spikeThreshold(new BigDecimal("100"), 8)
                .compareTo(new BigDecimal("800")));
    }

    @Test
    void spikeRatio_roundsToOneDecimal() {
        assertEquals(25.0, AnomalyRules.spikeRatio(new BigDecimal("2500"), new BigDecimal("100")));
        assertEquals(3.3, AnomalyRules.spikeRatio(new BigDecimal("1000"), new BigDecimal("300")));
    }

    @Test
    void spikeSeverity_highAt20xOrMore() {
        assertEquals("HIGH", AnomalyRules.spikeSeverity(20));   // ranh giới
        assertEquals("MEDIUM", AnomalyRules.spikeSeverity(19.9));
        assertEquals("MEDIUM", AnomalyRules.spikeSeverity(8));
    }

    @Test
    void isConcentrated_atOrAboveThreshold() {
        assertTrue(AnomalyRules.isConcentrated(0.50, 0.5));    // ranh giới
        assertTrue(AnomalyRules.isConcentrated(0.73, 0.5));
        assertFalse(AnomalyRules.isConcentrated(0.49, 0.5));
    }

    @Test
    void concentrationSeverity_highAt80pctOrMore() {
        assertEquals("HIGH", AnomalyRules.concentrationSeverity(0.8));   // ranh giới
        assertEquals("MEDIUM", AnomalyRules.concentrationSeverity(0.79));
    }
}
