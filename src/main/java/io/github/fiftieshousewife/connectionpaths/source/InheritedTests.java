package io.github.fiftieshousewife.connectionpaths.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A test method is run by its own class, if that class isn't abstract, and by every test class below it that doesn't
 * declare a method of the same name and parameters, with that class's set-up. A test runner names it by the class
 * that runs it, so it is listed under each.
 */
public final class InheritedTests {

    private InheritedTests() {
    }

    static void spread(final SourceModel model, final Set<String> abstractTypes) {
        final List<Map.Entry<MethodKey, SourceModel.Location>> declared =
            new ArrayList<>(model.tests().all().entrySet());
        final List<String> testTypes = model.tests().testTypes().stream().sorted().toList();
        for (final Map.Entry<MethodKey, SourceModel.Location> test : declared) {
            final MethodKey method = test.getKey();
            final List<String> runners = testTypes.stream()
                .filter(type -> !abstractTypes.contains(type) && model.types().ancestors(type).contains(method.type()))
                .filter(type -> !model.getDeclared().contains(method.inType(type)))
                .toList();
            runners.forEach(type -> model.tests().inheritedTest(method.inType(type), method, test.getValue()));
            if (abstractTypes.contains(method.type()) && !runners.isEmpty()) {
                model.tests().dropTest(method);
            }
        }
    }
}
