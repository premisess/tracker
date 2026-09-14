package com.fitness.tracker.service;

import com.fitness.tracker.service.RunMetricsCalculator.Point;

import java.util.List;

/** Google's encoded polyline format (precision 5): a compact string the map decodes into a route line. */
public final class PolylineCodec {

    private PolylineCodec() {
    }

    public static String encode(List<Point> points) {
        StringBuilder out = new StringBuilder();
        long previousLat = 0;
        long previousLng = 0;
        for (Point p : points) {
            long lat = Math.round(p.lat() * 1e5);
            long lng = Math.round(p.lng() * 1e5);
            encodeValue(lat - previousLat, out);
            encodeValue(lng - previousLng, out);
            previousLat = lat;
            previousLng = lng;
        }
        return out.toString();
    }

    private static void encodeValue(long value, StringBuilder out) {
        long v = value < 0 ? ~(value << 1) : (value << 1);
        while (v >= 0x20) {
            out.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        out.append((char) (v + 63));
    }
}
