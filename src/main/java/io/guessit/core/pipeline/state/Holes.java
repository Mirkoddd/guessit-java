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

        public boolean isEmpty() { var v = value(); return v != null && !v.isEmpty(); }

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
                if (!left.raw().isEmpty()) newRet.add(left);
                if (!right.raw().isEmpty()) newRet.add(right);
                return;
            }

            if (m.span().end() >= h.span().end() && m.span().start() < h.span().end()) {
                var cropped = h.withBounds(h.span().start(), m.span().start());
                if (!cropped.raw().isEmpty()) newRet.add(cropped);
                return;
            }

            if (m.span().start() <= h.span().start() && m.span().end() > h.span().start()) {
                var cropped = h.withBounds(m.span().end(), h.span().end());
                if (!cropped.raw().isEmpty()) newRet.add(cropped);
                return;
            }

            newRet.add(h);
        }

        public List<Hole> split(String seps) {
            var ret = new ArrayList<Hole>();
            var rawStr = raw();
            int i = 0;
            while (i < rawStr.length()) {
                while (i < rawStr.length() && seps.indexOf(rawStr.charAt(i)) >= 0) i++;
                int s = i;
                while (i < rawStr.length() && seps.indexOf(rawStr.charAt(i)) < 0) i++;
                if (s < i) {
                    var sub = withBounds(span.start() + s, span.start() + i);
                    if (sub.isEmpty()) ret.add(sub);
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

        int currentStart = -1;

        for (var pos = start; pos < end; pos++) {
            boolean inM = inMatch(active, pos);

            if (currentStart != -1 && seps != null && pos < input.length() && seps.indexOf(input.charAt(pos)) >= 0) {
                ret.add(new Hole(new Span(currentStart, pos, input.substring(currentStart, pos)), formatter));
                currentStart = -1;
            } else if (!inM && currentStart == -1) {
                currentStart = pos;
            } else if (inM && currentStart != -1) {
                ret.add(new Hole(new Span(currentStart, pos, input.substring(currentStart, pos)), formatter));
                currentStart = -1;
            }
        }

        if (currentStart != -1) {
            ret.add(new Hole(new Span(currentStart, end, input.substring(currentStart, end)), formatter));
        }

        ret.removeIf(h -> { var v = h.value(); return v == null || v.isEmpty(); });
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

    private static boolean inMatch(List<Match> active, int pos) {
        for (var m : active) {
            if (m.span().start() <= pos && pos < m.span().end()) return true;
        }
        return false;
    }
}