package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.XmlContexts;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The Spring profiles a bean in a {@code <beans profile="...">} block is made for: a step of its own between its
 * file's context and the bean, {@code ApplicationContext#<profile dev of spring/app.xml>}, which runs only where those
 * profiles are active. A block's profiles are any of its names, {@code dev,qa}, or a name not active, {@code !prod};
 * blocks inside one another must all hold; with no profile active, Spring's {@code default} is.
 */
public final class XmlProfiles {

    private static final String PREFIX = "<profile ";
    private static final String OF = " of ";
    private static final String AND = "; ";

    private XmlProfiles() {
    }

    static MethodKey stepOf(final String file, final List<String> profiles) {
        final MethodKey context = XmlContexts.contextOf(file);
        return new MethodKey(context.type(), PREFIX + String.join(AND, profiles) + OF
            + XmlContexts.pathOf(file) + ">", "");
    }

    /** Whether a step of a chain, as the facts write it, runs with these profiles active. */
    public static boolean runsWith(final String step, final List<String> active) {
        return conditionsIn(step).map(conditions -> Arrays.stream(conditions.split(AND))
            .allMatch(block -> holds(block, active.isEmpty() ? List.of("default") : active))).orElse(true);
    }

    private static Optional<String> conditionsIn(final String step) {
        final int at = step.indexOf("#" + PREFIX);
        final int of = step.lastIndexOf(OF);
        return at < 0 || of < at ? Optional.empty() : Optional.of(step.substring(at + 1 + PREFIX.length(), of));
    }

    private static boolean holds(final String block, final List<String> active) {
        return Arrays.stream(block.split("[,\\s]+")).filter(name -> !name.isEmpty())
            .anyMatch(name -> name.startsWith("!") ? !active.contains(name.substring(1)) : active.contains(name));
    }
}
