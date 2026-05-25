package io.guessit.rules.post;

import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.contracts.PostProcessor;

import java.util.List;

/**
 * Drops every match flagged {@link Match#isPrivate}. Private
 * matches exist only as scaffolding for other extractors (anchors, guards,
 * intermediate signals); they must not appear in the final result.
 */
@SuppressWarnings("JavadocReference")
public final class PrivateRemover implements PostProcessor {
    @Override
    public String description() {
        return "drop scaffolding / private matches";
    }

    @Override
    public void process(ParseContext ctx) {
        List<Match> privates = ctx.matches.all().filter(Match::isPrivate).toList();
        for (var m : privates) ctx.matches.remove(m);
    }
}
