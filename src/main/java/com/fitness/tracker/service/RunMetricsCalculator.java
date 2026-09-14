package com.fitness.tracker.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns raw GPS fixes into distance, moving time, splits, elevation gain and calories.
 * The distance filtering mirrors createActivityTracker in the frontend's utils/geo.js,
 * so the numbers a runner watched live are the numbers that get saved.
 */
public final class RunMetricsCalculator {

    /** One GPS fix. {@code seg} increases each time the activity is resumed after a pause. */
    public record Point(double lat, double lng, Double alt, long t, Double acc, int seg) {
    }

    public record Split(int index, int distanceM, int durationSec, int paceSecPerKm, int elevationGainM) {
    }

    public record ElevationSample(int distanceM, double altitudeM) {
    }

    public record Result(double distanceMeters,
                         int movingTimeSec,
                         int elevationGainM,
                         Integer avgPaceSecPerKm,
                         List<Point> track,
                         List<Split> splits,
                         List<ElevationSample> elevationProfile) {
    }

    private static final double EARTH_RADIUS_M = 6371008.8;

    // A fix vaguer than this can't place you on the right street.
    static final double MAX_ACCURACY_M = 50;

    // After this many "jumps" in a row, the old anchor was the bad fix; trust the new position.
    static final int MAX_REJECTED_IN_A_ROW = 3;

    // Altitude from phone GPS wobbles by a few metres; changes smaller than this aren't climbing.
    static final double ELEVATION_NOISE_M = 3;

    // A trailing partial kilometre shorter than this isn't worth a split row.
    static final double MIN_PARTIAL_SPLIT_M = 50;

    static final int MAX_PROFILE_SAMPLES = 200;

    private RunMetricsCalculator() {
    }

    public static Result calculate(String type, List<Point> rawPoints) {
        double maxSpeed = maxSpeedFor(type);
        List<Point> points = rawPoints.stream()
                .filter(RunMetricsCalculator::isUsable)
                .sorted(Comparator.comparingLong(Point::t))
                .toList();

        List<Point> track = new ArrayList<>();
        List<Double> distanceAt = new ArrayList<>();
        List<Double> movingAt = new ArrayList<>();

        double distance = 0;
        double closedSegmentsSec = 0;
        Point anchor = null;
        Point segmentStart = null;
        Point lastInSegment = null;
        int rejectedInARow = 0;

        for (Point p : points) {
            if (anchor != null && p.seg() != anchor.seg()) {
                closedSegmentsSec += (lastInSegment.t() - segmentStart.t()) / 1000.0;
                anchor = null;
            }
            if (anchor == null) {
                anchor = p;
                segmentStart = p;
                lastInSegment = p;
                rejectedInARow = 0;
                track.add(p);
                distanceAt.add(distance);
                movingAt.add(closedSegmentsSec);
                continue;
            }

            double d = haversine(anchor, p);
            double dt = (p.t() - anchor.t()) / 1000.0;
            if (dt > 0 && d / dt > maxSpeed) {
                rejectedInARow++;
                if (rejectedInARow >= MAX_REJECTED_IN_A_ROW) {
                    anchor = p;
                    lastInSegment = p;
                    rejectedInARow = 0;
                    track.add(p);
                    distanceAt.add(distance);
                    movingAt.add(closedSegmentsSec + (p.t() - segmentStart.t()) / 1000.0);
                }
                continue;
            }
            rejectedInARow = 0;
            lastInSegment = p;
            if (d < movementThreshold(p.acc())) {
                continue;
            }

            distance += d;
            anchor = p;
            track.add(p);
            distanceAt.add(distance);
            movingAt.add(closedSegmentsSec + (p.t() - segmentStart.t()) / 1000.0);
        }

        double movingSec = lastInSegment == null ? 0 : closedSegmentsSec + (lastInSegment.t() - segmentStart.t()) / 1000.0;
        List<Double> gainAt = cumulativeElevationGain(track);
        double totalGain = gainAt.isEmpty() ? 0 : gainAt.get(gainAt.size() - 1);

        return new Result(
                distance,
                (int) Math.round(movingSec),
                (int) Math.round(totalGain),
                distance >= MIN_PARTIAL_SPLIT_M && movingSec > 0 ? (int) Math.round(movingSec / (distance / 1000.0)) : null,
                track,
                splits(distanceAt, movingAt, gainAt, distance, movingSec),
                elevationProfile(track, distanceAt, distance));
    }

    /** Standard MET values from the 2011 Compendium of Physical Activities, interpolated by speed. */
    public static double metFor(String type, double speedKmh) {
        return switch (type == null ? "" : type) {
            case "Walking" -> interpolate(WALKING_MET, speedKmh);
            case "Cycling" -> interpolate(CYCLING_MET, speedKmh);
            case "Hiking" -> 6.0;
            default -> interpolate(RUNNING_MET, speedKmh);
        };
    }

    public static int calories(String type, double distanceMeters, int movingTimeSec, double weightKg) {
        if (movingTimeSec <= 0) {
            return 0;
        }
        double hours = movingTimeSec / 3600.0;
        double speedKmh = (distanceMeters / 1000.0) / hours;
        return (int) Math.round(metFor(type, speedKmh) * weightKg * hours);
    }

    /**
     * Drops every point within {@code radiusM} of the start or the end, so a shared route
     * doesn't reveal where the runner lives. Returns an empty list if nothing is left.
     */
    public static List<Point> hideEnds(List<Point> track, int radiusM) {
        if (radiusM <= 0 || track.isEmpty()) {
            return track;
        }
        Point start = track.get(0);
        Point end = track.get(track.size() - 1);
        return track.stream()
                .filter(p -> haversine(start, p) >= radiusM && haversine(end, p) >= radiusM)
                .toList();
    }

    /** Ramer-Douglas-Peucker: removes points that don't change the drawn shape by more than toleranceM. */
    public static List<Point> simplify(List<Point> track, double toleranceM) {
        if (track.size() < 3) {
            return track;
        }
        boolean[] keep = new boolean[track.size()];
        keep[0] = true;
        keep[track.size() - 1] = true;
        ArrayList<int[]> stack = new ArrayList<>();
        stack.add(new int[]{0, track.size() - 1});
        while (!stack.isEmpty()) {
            int[] range = stack.remove(stack.size() - 1);
            double maxDistance = 0;
            int index = -1;
            for (int i = range[0] + 1; i < range[1]; i++) {
                double d = distanceToSegment(track.get(i), track.get(range[0]), track.get(range[1]));
                if (d > maxDistance) {
                    maxDistance = d;
                    index = i;
                }
            }
            if (index >= 0 && maxDistance > toleranceM) {
                keep[index] = true;
                stack.add(new int[]{range[0], index});
                stack.add(new int[]{index, range[1]});
            }
        }
        List<Point> result = new ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            if (keep[i]) {
                result.add(track.get(i));
            }
        }
        return result;
    }

    public static double haversine(Point a, Point b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLng = Math.toRadians(b.lng() - a.lng());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    static double maxSpeedFor(String type) {
        return switch (type == null ? "" : type) {
            case "Walking" -> 4.0;
            case "Hiking" -> 5.0;
            case "Cycling" -> 25.0;
            default -> 12.5;
        };
    }

    static double movementThreshold(Double accuracy) {
        double acc = accuracy == null ? 0 : accuracy;
        return Math.min(10, Math.max(3, acc * 0.5));
    }

    private static boolean isUsable(Point p) {
        return p.lat() >= -90 && p.lat() <= 90 && p.lng() >= -180 && p.lng() <= 180
                && (p.acc() == null || p.acc() <= MAX_ACCURACY_M);
    }

    private static List<Double> cumulativeElevationGain(List<Point> track) {
        List<Double> gainAt = new ArrayList<>(track.size());
        double gain = 0;
        Double reference = null;
        int segment = Integer.MIN_VALUE;
        for (Point p : track) {
            if (p.alt() != null) {
                if (p.seg() != segment || reference == null) {
                    segment = p.seg();
                    reference = p.alt();
                } else if (p.alt() - reference >= ELEVATION_NOISE_M) {
                    gain += p.alt() - reference;
                    reference = p.alt();
                } else if (reference - p.alt() >= ELEVATION_NOISE_M) {
                    reference = p.alt();
                }
            }
            gainAt.add(gain);
        }
        return gainAt;
    }

    private static List<Split> splits(List<Double> distanceAt, List<Double> movingAt, List<Double> gainAt,
                                      double totalDistance, double totalMovingSec) {
        List<Split> splits = new ArrayList<>();
        double previousCrossTime = 0;
        double previousCrossGain = 0;
        int nextKm = 1;
        for (int i = 1; i < distanceAt.size(); i++) {
            double d0 = distanceAt.get(i - 1);
            double d1 = distanceAt.get(i);
            while (d1 >= nextKm * 1000.0 && d1 > d0) {
                double fraction = (nextKm * 1000.0 - d0) / (d1 - d0);
                double crossTime = movingAt.get(i - 1) + fraction * (movingAt.get(i) - movingAt.get(i - 1));
                double crossGain = gainAt.get(i);
                int duration = (int) Math.round(crossTime - previousCrossTime);
                splits.add(new Split(nextKm, 1000, duration, duration, (int) Math.round(crossGain - previousCrossGain)));
                previousCrossTime = crossTime;
                previousCrossGain = crossGain;
                nextKm++;
            }
        }
        double remainder = totalDistance - (nextKm - 1) * 1000.0;
        if (remainder >= MIN_PARTIAL_SPLIT_M) {
            int duration = (int) Math.round(totalMovingSec - previousCrossTime);
            double lastGain = gainAt.isEmpty() ? 0 : gainAt.get(gainAt.size() - 1);
            splits.add(new Split(nextKm, (int) Math.round(remainder), duration,
                    (int) Math.round(duration / (remainder / 1000.0)), (int) Math.round(lastGain - previousCrossGain)));
        }
        return splits;
    }

    private static List<ElevationSample> elevationProfile(List<Point> track, List<Double> distanceAt, double totalDistance) {
        List<ElevationSample> samples = new ArrayList<>();
        double step = Math.max(1, totalDistance / MAX_PROFILE_SAMPLES);
        double lastIncluded = -step;
        for (int i = 0; i < track.size(); i++) {
            Point p = track.get(i);
            boolean isLast = i == track.size() - 1;
            if (p.alt() != null && (distanceAt.get(i) - lastIncluded >= step || isLast)) {
                samples.add(new ElevationSample((int) Math.round(distanceAt.get(i)), Math.round(p.alt() * 10) / 10.0));
                lastIncluded = distanceAt.get(i);
            }
        }
        return samples;
    }

    // Equirectangular projection is accurate to well under a metre over the few hundred metres RDP compares.
    private static double distanceToSegment(Point p, Point a, Point b) {
        double latRad = Math.toRadians((a.lat() + b.lat()) / 2);
        double ax = 0, ay = 0;
        double bx = Math.toRadians(b.lng() - a.lng()) * Math.cos(latRad) * EARTH_RADIUS_M;
        double by = Math.toRadians(b.lat() - a.lat()) * EARTH_RADIUS_M;
        double px = Math.toRadians(p.lng() - a.lng()) * Math.cos(latRad) * EARTH_RADIUS_M;
        double py = Math.toRadians(p.lat() - a.lat()) * EARTH_RADIUS_M;
        double lengthSquared = bx * bx + by * by;
        double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * bx + (py - ay) * by) / lengthSquared));
        double dx = px - t * bx;
        double dy = py - t * by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double interpolate(double[][] table, double speed) {
        if (speed <= table[0][0]) {
            return table[0][1];
        }
        for (int i = 1; i < table.length; i++) {
            if (speed <= table[i][0]) {
                double f = (speed - table[i - 1][0]) / (table[i][0] - table[i - 1][0]);
                return table[i - 1][1] + f * (table[i][1] - table[i - 1][1]);
            }
        }
        return table[table.length - 1][1];
    }

    // {speed km/h, MET}
    private static final double[][] RUNNING_MET = {
            {6.4, 6.0}, {8.0, 8.3}, {8.4, 9.0}, {9.7, 9.8}, {10.8, 10.5}, {11.3, 11.0}, {12.1, 11.8},
            {12.9, 12.3}, {13.8, 12.8}, {14.5, 14.5}, {16.1, 16.0}, {17.7, 19.0}, {19.3, 19.8}, {20.9, 23.0}};
    private static final double[][] WALKING_MET = {
            {3.2, 2.8}, {4.0, 3.0}, {4.8, 3.5}, {5.6, 4.3}, {6.4, 5.0}, {7.2, 7.0}, {8.0, 8.3}};
    private static final double[][] CYCLING_MET = {
            {10.0, 4.0}, {17.6, 6.8}, {20.9, 8.0}, {24.1, 10.0}, {28.1, 12.0}, {32.2, 15.8}};
}
