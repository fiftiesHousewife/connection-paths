package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.Frameworks;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.TestModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Where a test would connect, from the settings on the test classpath: to a host; on this machine; nowhere, as no
// setting the test runs with gives the address; or to something only known at run time.
final class TestAddresses {

    static final String HOST = "host";
    static final String LOCAL = "local";
    static final String NONE = "none";
    static final String RUN_TIME = "run-time";

    private final Map<String, Address> byPoint;
    private final SettingsFiles settings;
    private final ArgumentTracer arguments;
    private final TestModel tests;

    TestAddresses(final SourceModel model, final SettingsFiles settings) {
        this.settings = settings.onTheTestClasspath();
        this.arguments = new ArgumentTracer(model);
        this.tests = model.tests();
        this.byPoint = model.getPoints().stream().collect(Collectors.toMap(
            point -> point.method() + " " + point.kind(), point -> model.callsOf(point).get(0).address(),
            (first, second) -> first));
    }

    // Where a reach's test would connect. A point whose address is one of its parameters is followed back along the
    // test's chain, to the argument each caller passes, as far as the test.
    String of(final List<MethodKey> chain, final String kind) {
        final String graded = graded(chain, kind);
        return RUN_TIME.equals(graded) && chain.size() > 1 ? fromTheTest(chain, kind).orElse(graded) : graded;
    }

    // Where the reach's own address leaves it unknown, what the test's call into its chain says, if that names a host
    // or this machine: an address handed in, held by a builder and read back as the connection is made.
    private Optional<String> fromTheTest(final List<MethodKey> chain, final String kind) {
        return arguments.atTheTestsCall(chain.get(0), chain.get(1), kind)
            .map(address -> classify(settings.withValue(address), kind))
            .filter(grade -> HOST.equals(grade) || LOCAL.equals(grade));
    }

    private String graded(final List<MethodKey> chain, final String kind) {
        final Address address = alongTheChain(chain, byPoint.get(chain.get(chain.size() - 1) + " " + kind));
        if (address.isRelativePath() && servesOnAPort(chain.get(0).type())) {
            return LOCAL;
        }
        return classify(settings.withValue(address), kind);
    }

    private Address alongTheChain(final List<MethodKey> chain, final Address atThePoint) {
        Address address = atThePoint;
        for (int step = chain.size() - 1; step > 0 && ArgumentTracer.parameterOf(address).isPresent(); step--) {
            final String parameter = ArgumentTracer.parameterOf(address).orElseThrow();
            address = arguments.passedBy(chain.get(step - 1), chain.get(step), parameter).orElse(address);
            if (ArgumentTracer.parameterOf(address).filter(parameter::equals).isPresent()) {
                break;
            }
        }
        return address;
    }

    // With no settings known on the test classpath, which ones a test runs with is unknown: never "none".
    private String classify(final Address address, final String kind) {
        if (hasNoValue(address)) {
            return noValue();
        }
        if (Locality.of(address, kind).isPresent() || "process".equals(kind)) {
            return LOCAL;
        }
        return PointGroups.hostOf(address).isPresent() ? HOST : RUN_TIME;
    }

    // A test class that runs the application on a port of this machine, where a relative path is sent.
    private boolean servesOnAPort(final String testClass) {
        return tests.frameworkOf(testClass).map(TestModel.Framework::annotation).filter(Frameworks::servesOnAPort)
            .isPresent();
    }

    private String noValue() {
        return settings.isEmpty() ? RUN_TIME : NONE;
    }

    // A setting that no file on the test classpath gives a value, or only a placeholder for one.
    private static boolean hasNoValue(final Address address) {
        return Address.SETTING.equals(address.getSource())
            && (address.getSettingValue() == null || address.isPlaceholder());
    }
}
