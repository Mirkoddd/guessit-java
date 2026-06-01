package io.guessit.rules.post;

import io.guessit.core.pipeline.state.*;
import io.guessit.core.pipeline.contracts.PostProcessor;

import java.util.*;
import java.util.function.Predicate;

/**
 * For each property name, the first filepart's values win; later fileparts
 * only keep values that were already seen in an earlier filepart.
 *
 * <p>This is a port of Python guessit's {@code RemoveAmbiguous} post-processor.
 */
public class RemoveAmbiguous implements PostProcessor {
    protected final Predicate<Match> predicate;
    protected final boolean reverseFileparts;
    protected final Comparator<Match> tieBreak;

    public RemoveAmbiguous() {
        this(_ -> true, false, (_, _) -> 0);
    }

    public RemoveAmbiguous(Predicate<Match> predicate, boolean reverseFileparts, Comparator<Match> tieBreak) {
        this.predicate = predicate;
        this.reverseFileparts = reverseFileparts;
        this.tieBreak = tieBreak;
    }

    @Override
    public String description() {
        return "drop ambiguous low-confidence matches";
    }

    @Override
    public void process(ParseContext ctx) {
        // Mirror python: iterate fileparts in marker_sorted order (most-
        // valuable filepart first), so its values win when later fileparts
        // have differently-valued same-named matches.
        var pathsSorted = ctx.markers.stream()
                .filter(m -> m.type() == MarkerType.PATH)
                .toList();
        var sorted = Markers.markerSorted(pathsSorted, ctx.matches);
        var paths = reverseFileparts ? sorted.reversed() : sorted;

        var perFilepart = new ArrayList<List<Match>>();
        for (var fp : paths) {
            var inFp = ctx.matches.inMarker(fp)
                    .filter(m -> !m.isPrivate())
                    .filter(predicate)
                    .sorted(tieBreak)
                    .toList();
            perFilepart.add(inFp);
        }
        applyBucketDedup(ctx, perFilepart);
    }

    /**
     * For each filepart's sorted match list, the first filepart's values per
     * name win; later fileparts only retain matches whose value was already
     * seen earlier. Shared between {@link RemoveAmbiguous} and
     * {@link RemoveLessSpecificSeasonEpisode}.
     */
    protected static void applyBucketDedup(ParseContext ctx, List<List<Match>> perFilepart) {
        var seenNames = new HashSet<MatchName>();
        var values = new EnumMap<MatchName, Set<Object>>(MatchName.class);
        var toRemove = new ArrayList<Match>();
        for (var inFp : perFilepart) {
            var fpNames = new HashSet<MatchName>();
            for (var m : inFp) {
                fpNames.add(m.name());
                var bucket = values.computeIfAbsent(m.name(), _ -> new LinkedHashSet<>());
                if (seenNames.contains(m.name())) {
                    if (!bucket.contains(m.value())) toRemove.add(m);
                } else {
                    bucket.add(m.value());
                }
            }
            seenNames.addAll(fpNames);
        }
        for (var m : toRemove) ctx.matches.remove(m);
    }
}