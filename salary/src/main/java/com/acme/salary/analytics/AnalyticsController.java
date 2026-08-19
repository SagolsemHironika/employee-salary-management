package com.acme.salary.analytics;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }

    @GetMapping("/analytics/by-dimension")
    public List<DimensionBreakdown> byDimension(@RequestParam String groupBy) {
        return analyticsService.byDimension(groupBy);
    }

    @GetMapping("/analytics/distribution")
    public List<DistributionBucket> distribution() {
        return analyticsService.distribution();
    }
}
