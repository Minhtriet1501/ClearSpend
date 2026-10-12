package com.clearspend.anomaly;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AnomalyController {
    private final AnomalyService anomalyService;
    private final FlagRepository flagRepo;

    @PostMapping("/internal/anomaly/run")
    public Map<String, Object> run() {
        return Map.of("SPIKE_VS_MEDIAN", anomalyService.detectSpikes(),
                "VENDOR_CONCENTRATION", anomalyService.detectVendorConcentration());
    }

    @GetMapping("/flags")
    public List<Flag> flags() {
        return flagRepo.findTop50ByOrderByCreatedAtDesc();
    }

}
