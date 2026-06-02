package io.guessit.core.pipeline.state;

import io.guessit.core.text.Span;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class Holes {
    private Holes() {}

    public record Hole(Span span, UnaryOperator<String> formatter) {

        public String raw() { return span.raw(); }
        public String value() { return formatter == null ? raw() : formatter.apply(raw()); }

        public boolean isNotEmpty() { var v = value(); return v != null && !v.isEmpty(); }

        public int start() { return span.start(); }
        public int end() { return span.end(); }

        public Hole withBounds(int s, int e) {
            int localStart = s - span.start();
            int localEnd = e - span.start();
            return new Hole(new Span(s, e, raw().substring(localStart, localEnd)), formatter);
        }

        public List<Hole> crop(List<Marker> markers) {
            var ret = new ArrayList<Hole>();
            ret.add(this);
            for (var m : markers) {
                var newRet = new ArrayList<Hole>();
                for (var h : ret) applyMarkerToHole(m, h, newRet);
                ret = newRet;
            }
            return ret;
        }

        private void applyMarkerToHole(Marker m, Hole h, List<Hole> newRet) {
            if (!m.span().overlaps(h.span())) {
                newRet.add(h);
                return;
            }

            if (m.span().contains(h.span())) return;

            if (h.span().contains(m.span())) {
                var left = h.withBounds(h.span().start(), m.span().start());
                var right = h.withBounds(m.span().end(), h.span().end());
                if (left.isNotEmpty()) newRet.add(left);
                if (right.isNotEmpty()) newRet.add(right);
                return;
            }

            if (m.span().end() >= h.span().end() && m.span().start() < h.span().end()) {
                var cropped = h.withBounds(h.span().start(), m.span().start());
                if (cropped.isNotEmpty()) newRet.add(cropped);
                return;
            }

            if (m.span().start() <= h.span().start() && m.span().end() > h.span().start()) {
                var cropped = h.withBounds(m.span().end(), h.span().end());
                if (cropped.isNotEmpty()) newRet.add(cropped);
                return;
            }

            newRet.add(h);
        }

        public List<Hole> split(String seps) {
            if (seps == null || seps.isEmpty()) return List.of(this);

            var ret = new ArrayList<Hole>();
            var rawStr = raw();
            int i = 0;
            while (i < rawStr.length()) {
                while (i < rawStr.length() && seps.indexOf(rawStr.charAt(i)) >= 0) i++;
                int s = i;
                while (i < rawStr.length() && seps.indexOf(rawStr.charAt(i)) < 0) i++;
                if (s < i) {
                    var sub = withBounds(span.start() + s, span.start() + i);
                    if (sub.isNotEmpty()) ret.add(sub);
                }
            }
            return ret;
        }
    }

    public static List<Hole> compute(String input, int start, int end,
                                     List<Match> allMatches,
                                     Predicate<Match> ignore,
                                     String seps,
                                     UnaryOperator<String> formatter) {

        var active = collectActiveMatches(allMatches, ignore, start, end);
        var ret = new ArrayList<Hole>();

        int cursor = start;

        for (var m : active) {
            int mStart = Math.max(cursor, m.span().start());
            if (mStart > cursor) {
                ret.add(new Hole(new Span(cursor, mStart, input.substring(cursor, mStart)), formatter));
            }
            cursor = Math.max(cursor, m.span().end());
        }

        if (cursor < end) {
            ret.add(new Hole(new Span(cursor, end, input.substring(cursor, end)), formatter));
        }

        if (seps != null && !seps.isEmpty()) {
            var splitRet = new ArrayList<Hole>();
            for (var h : ret) {
                splitRet.addAll(h.split(seps));
            }
            ret = splitRet;
        }

        ret.removeIf(h -> !h.isNotEmpty());
        return ret;
    }

    private static List<Match> collectActiveMatches(List<Match> allMatches, Predicate<Match> ignore,
                                                    int start, int end) {
        var matches = new ArrayList<>(allMatches);
        matches.sort(Comparator.comparingInt(m -> m.span().start()));
        var active = new ArrayList<Match>();
        var target = new Span(start, end, "");

        for (var m : matches) {
            if (ignore != null && ignore.test(m)) continue;
            if (!m.span().overlaps(target)) continue;
            active.add(m);
        }
        return active;
    }
}