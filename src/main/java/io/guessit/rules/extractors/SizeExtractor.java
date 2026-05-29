package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Validators;
import io.guessit.api.models.Size;
import io.guessit.core.text.patterns.SizePatterns;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts {@code size} (123MB, 4.5GB, …).
 */
public final class SizeExtractor implements Extractor {

    public static final String EXTRACTOR_NAME = "size";
    private static final String GRP_SIZE = "val";
    private static final String TAG_RELEASE_GROUP_PREFIX = "release-group-prefix";

    private static final Pattern PATTERN = SizePatterns.buildSizePattern(GRP_SIZE);

    @Override
    public String name() {
        return EXTRACTOR_NAME;
    }

    @Override
    public String description() {
        return "size (123MB, 4.5GB, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = PATTERN.matcher(input);

        while (m.find()) {
            var raw = m.group(GRP_SIZE);
            var head = new Match(MatchName.SIZE, null, m.start(GRP_SIZE), m.end(GRP_SIZE), raw, priority(), Set.of(), false);

            if (seps.test(head)) {
                ctx.matches.add(new Match(MatchName.SIZE, Size.fromString(raw), m.start(GRP_SIZE), m.end(GRP_SIZE), raw,
                        priority(), Set.of(TAG_RELEASE_GROUP_PREFIX), false));
            }
        }
    }
}