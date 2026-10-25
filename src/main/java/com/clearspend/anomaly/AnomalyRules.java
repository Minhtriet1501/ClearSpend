package com.clearspend.anomaly;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure anomaly decision rules — no DB, no Spring. Unit-testable in isolation. */
public final class AnomalyRules {
    private AnomalyRules() {}

    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";

    /** Rule 1 threshold: spend above k × the agency median is a "spike". */
    public static BigDecimal spikeThreshold(BigDecimal median, double k) {
        return median.multiply(BigDecimal.valueOf(k));
    }

    /** How many times the median an amount is (amount ÷ median), 1 decimal. */
    public static double spikeRatio(BigDecimal amount, BigDecimal median) {
        return amount.divide(median, 1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Spike severity: HIGH when ≥ 20× the median, else MEDIUM. */
    public static String spikeSeverity(double ratio) {
        return ratio >= 20 ? HIGH : MEDIUM;
    }

    /** Rule 2: a vendor is over-concentrated when its share of agency spend ≥ threshold. */
    public static boolean isConcentrated(double share, double threshold) {
        return share >= threshold;
    }

    /** Concentration severity: HIGH when a vendor takes ≥ 80% of agency spend, else MEDIUM. */
    public static String concentrationSeverity(double share) {
        return share >= 0.8 ? HIGH : MEDIUM;
    }
}
