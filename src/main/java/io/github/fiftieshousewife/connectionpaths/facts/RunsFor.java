package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.List;
import java.util.Set;

// Whether a chain of calls runs as a test does: a chain into a type that only other test classes' own code creates
// never runs as this test does.
final class RunsFor {

    private final SourceModel model;
    private final Set<String> createdOutsideTests;

    RunsFor(final SourceModel model) {
        this.model = model;
        this.createdOutsideTests = model.created(false);
    }

    boolean test(final MethodKey test, final List<String> chain) {
        return chain.stream().map(step -> step.substring(0, step.indexOf('#')))
            .filter(type -> !createdOutsideTests.contains(type))
            .allMatch(type -> model.tests().mayBeCreatedFor(test.type(), type));
    }
}
