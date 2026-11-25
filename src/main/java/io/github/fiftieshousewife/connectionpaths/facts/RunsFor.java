package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Whether a chain of calls runs as a test does: each type on it that the program creates objects of must be created
// as the test's class runs, by its own code or code that then runs; a type nothing creates, such as an abstract class
// or one only of static methods, is called as it is.
final class RunsFor {

    private final SourceModel model;
    private final Set<String> createdAtAll;
    private final Map<String, Set<String>> createdFor = new HashMap<>();

    RunsFor(final SourceModel model) {
        this.model = model;
        this.createdAtAll = model.created();
    }

    boolean test(final MethodKey test, final List<String> chain) {
        final Set<String> created = createdFor.computeIfAbsent(test.type(), model::createdFor);
        return chain.stream().map(step -> step.substring(0, step.indexOf('#')))
            .allMatch(type -> created.contains(type) || !createdAtAll.contains(type));
    }
}
