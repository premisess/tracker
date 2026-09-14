package com.fitness.tracker.dto;

import com.fitness.tracker.service.RunMetricsCalculator.ElevationSample;
import com.fitness.tracker.service.RunMetricsCalculator.Split;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RunDetailResponse {
    private RunSummaryResponse summary;
    private String notes;
    private List<String> tags;
    // What others would see: the route minus the privacy zone. Null if the zone hides all of it.
    private String sharePolyline;
    private Integer privacyMeters;
    private List<Split> splits;
    private List<ElevationSample> elevationProfile;
}
