package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Where a test's chain would run a mock instead of the real code: the first call to an instance method of a type the
// test puts a mock in place of, in its own code, what runs with it, or the test-source helpers they call. A static
// method or a constructor of a mocked type still runs its own code.
final class MockedChains {

    private static final int HELPER_DEPTH = 3;

    private final SourceModel model;

    MockedChains(final SourceModel model) {
        this.model = model;
    }

    Optional<MethodKey> throughMock(final MethodKey test, final List<MethodKey> chain) {
        final Set<String> mocked = mockedFor(test);
        return mocked.isEmpty() ? Optional.empty()
            : chain.stream().skip(1).filter(step -> runsAMock(step, mocked)).findFirst();
    }

    private Set<String> mockedFor(final MethodKey test) {
        final Set<MethodKey> code = new HashSet<>(model.tests().setUpOf(test.type()));
        code.add(model.tests().declarationOf(test));
        Set<MethodKey> latest = Set.copyOf(code);
        for (int depth = 0; depth < HELPER_DEPTH && !latest.isEmpty(); depth++) {
            latest = latest.stream().flatMap(m -> model.getCalls().getOrDefault(m, Set.of()).stream())
                .filter(callee -> model.tests().isTestType(callee.type())).filter(code::add)
                .collect(Collectors.toSet());
        }
        return code.stream().flatMap(m -> model.tests().mockedBy(m).stream()).collect(Collectors.toSet());
    }

    private boolean runsAMock(final MethodKey step, final Set<String> mocked) {
        if (step.isConstructor() || step.isInitialisers() || model.tests().isStaticMethod(step)) {
            return false;
        }
        return mocked.contains(step.type()) || model.types().ancestors(step.type()).stream().anyMatch(mocked::contains);
    }
}
