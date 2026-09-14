package com.fitness.tracker.service;

import com.fitness.tracker.service.RunMetricsCalculator.Point;
import com.fitness.tracker.service.RunMetricsCalculator.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunMetricsCalculatorTest {

    // One degree of latitude is ~111,195 m, so this is ~3 m per fix northwards.
    private static final double LAT_STEP_3M = 3.0 / 111_195.0;

    /** A steady northward run: one fix per second at {@code metersPerSecond}. */
    private static List<Point> straightRun(int seconds, double metersPerSecond, int segment, long startMs) {
        List<Point> points = new ArrayList<>();
        double step = metersPerSecond / 111_195.0;
        for (int s = 0; s <= seconds; s++) {
            points.add(new Point(51.5 + s * step, -0.12, 20.0, startMs + s * 1000L, 5.0, segment));
        }
        return points;
    }

    @Test
    void steadyRunGivesDistanceTimeAndPace() {
        // 1,200 s at 3 m/s = 3,600 m, i.e. 5:33 per km.
        Result r = RunMetricsCalculator.calculate("Running", straightRun(1200, 3.0, 0, 0));

        assertEquals(3600, r.distanceMeters(), 15);
        assertEquals(1200, r.movingTimeSec());
        assertEquals(333, r.avgPaceSecPerKm(), 2);
        assertEquals(4, r.splits().size(), "3 full km plus a 600 m partial");
        assertEquals(1000, r.splits().get(0).distanceM());
        assertEquals(333, r.splits().get(0).durationSec(), 2);
        assertEquals(600, r.splits().get(3).distanceM(), 15);
    }

    @Test
    void standingStillWithJitterAddsNoDistance() {
        List<Point> points = new ArrayList<>();
        for (int s = 0; s < 300; s++) {
            // Wobble up to ~2 m around one spot, below the 3 m movement threshold.
            double wobble = (s % 2 == 0 ? 1 : -1) * (1.0 / 111_195.0);
            points.add(new Point(51.5 + wobble, -0.12, 20.0, s * 1000L, 4.0, 0));
        }
        Result r = RunMetricsCalculator.calculate("Running", points);

        assertEquals(0, r.distanceMeters(), 0.001);
        assertTrue(r.splits().isEmpty());
        assertNull(r.avgPaceSecPerKm());
    }

    @Test
    void singleGpsJumpIsIgnored() {
        List<Point> points = new ArrayList<>(straightRun(100, 3.0, 0, 0));
        // A fix 2 km away one second later is physically impossible on foot.
        points.add(50, new Point(51.52, -0.12, 20.0, 50_500L, 5.0, 0));

        Result r = RunMetricsCalculator.calculate("Running", points);

        assertEquals(300, r.distanceMeters(), 10);
    }

    @Test
    void inaccurateFixesAreDropped() {
        List<Point> points = new ArrayList<>(straightRun(100, 3.0, 0, 0));
        points.add(new Point(51.6, -0.2, 20.0, 101_000L, 400.0, 0));

        Result r = RunMetricsCalculator.calculate("Running", points);

        assertEquals(300, r.distanceMeters(), 10);
    }

    @Test
    void pausedTimeIsNotMovingTime() {
        List<Point> points = new ArrayList<>(straightRun(600, 3.0, 0, 0));
        double resumeLat = points.get(points.size() - 1).lat();
        // Paused for 10 minutes, then another 600 s from the same spot.
        for (Point p : straightRun(600, 3.0, 1, 1_200_000L)) {
            points.add(new Point(resumeLat + (p.lat() - 51.5), p.lng(), p.alt(), p.t(), p.acc(), p.seg()));
        }

        Result r = RunMetricsCalculator.calculate("Running", points);

        assertEquals(1200, r.movingTimeSec());
        assertEquals(3600, r.distanceMeters(), 20);
    }

    @Test
    void elevationGainIgnoresNoiseButCountsRealClimbs() {
        List<Point> points = new ArrayList<>();
        for (int s = 0; s <= 400; s++) {
            double noise = (s % 2 == 0) ? 1.5 : -1.5;
            double climb = s * 0.1; // 40 m over the run
            points.add(new Point(51.5 + s * LAT_STEP_3M, -0.12, 100 + climb + noise, s * 1000L, 5.0, 0));
        }
        Result r = RunMetricsCalculator.calculate("Running", points);

        assertEquals(40, r.elevationGainM(), 6);
    }

    @Test
    void metInterpolatesBetweenCompendiumSpeeds() {
        assertEquals(9.8, RunMetricsCalculator.metFor("Running", 9.7), 0.001);
        assertEquals(6.0, RunMetricsCalculator.metFor("Running", 3.0), 0.001);
        double between = RunMetricsCalculator.metFor("Running", 10.25);
        assertTrue(between > 9.8 && between < 10.5);
        // 30 min at 9.7 km/h, 70 kg: 9.8 MET x 70 kg x 0.5 h = 343 kcal.
        assertEquals(343, RunMetricsCalculator.calories("Running", 4850, 1800, 70));
    }

    @Test
    void hideEndsRemovesPointsNearStartAndFinish() {
        List<Point> track = straightRun(1000, 1.0, 0, 0); // 1 km, one point per metre
        List<Point> shared = RunMetricsCalculator.hideEnds(track, 200);

        assertFalse(shared.isEmpty());
        assertTrue(RunMetricsCalculator.haversine(track.get(0), shared.get(0)) >= 200);
        assertTrue(RunMetricsCalculator.haversine(track.get(track.size() - 1), shared.get(shared.size() - 1)) >= 200);
        assertTrue(RunMetricsCalculator.hideEnds(straightRun(100, 1.0, 0, 0), 200).isEmpty());
    }

    @Test
    void simplifyKeepsShapeOfAStraightLineWithTwoPoints() {
        List<Point> simplified = RunMetricsCalculator.simplify(straightRun(500, 3.0, 0, 0), 3);
        assertEquals(2, simplified.size());
    }

    @Test
    void polylineMatchesGoogleReferenceEncoding() {
        List<Point> points = List.of(
                new Point(38.5, -120.2, null, 0, null, 0),
                new Point(40.7, -120.95, null, 1, null, 0),
                new Point(43.252, -126.453, null, 2, null, 0));
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", PolylineCodec.encode(points));
    }
}
