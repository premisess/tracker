package com.fitness.tracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** One GPS fix from the browser's Geolocation API. */
@Data
public class RunPointDTO {
    @NotNull(message = "GPS point is missing a latitude")
    @DecimalMin(value = "-90", message = "GPS latitude is out of range")
    @DecimalMax(value = "90", message = "GPS latitude is out of range")
    private Double lat;

    @NotNull(message = "GPS point is missing a longitude")
    @DecimalMin(value = "-180", message = "GPS longitude is out of range")
    @DecimalMax(value = "180", message = "GPS longitude is out of range")
    private Double lng;

    // Metres above sea level; many devices don't report it.
    private Double alt;

    // Epoch milliseconds.
    @NotNull(message = "GPS point is missing a timestamp")
    @Positive(message = "GPS timestamp is invalid")
    private Long t;

    // Accuracy radius in metres.
    @PositiveOrZero(message = "GPS accuracy is invalid")
    private Double acc;

    // Increases each time the activity is resumed after a pause.
    @NotNull(message = "GPS point is missing its segment")
    @PositiveOrZero(message = "GPS segment is invalid")
    private Integer seg;
}
