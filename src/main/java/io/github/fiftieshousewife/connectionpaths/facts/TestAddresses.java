package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.address.SettingAnnotations;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.Frameworks;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.TestModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Where a test would connect, from the settings on the test classpath: to a host; on this machine; nowhere, as no
// setting the test runs with gives the address; or to something only known at run time.
final class TestAddresses {

    static final String HOST = "host";
    static final String LOCAL = "local";
    static final String NONE = "none";
    static final String RUN_TIME = "run-time";

    private final Map<String, Address> byPoint;
    private final Map<String, List<Address>> everyCallOf;
    private final Set<String> listening;
    private final SettingsFiles settings;
    private final ArgumentTracer arguments;
    private final TestModel tests;
    private final ClientsWithoutAnAddress bareClients;

    TestAddresses(final SourceModel model, final SettingsFiles settings) {
        this.settings = settings.onTheTestClasspath();
        this.arguments = new ArgumentTracer(model);
        this.tests = model.tests();
        this.bareClients = new ClientsWithoutAnAddress(model);
        this.byPoint = model.getPoints().stream().collect(Collectors.toMap(
            point -> point.method() + " " + point.kind(), point -> model.callsOf(point).get(0).address(),
            (first, second) -> first));
        this.everyCallOf = model.getPoints().stream().collect(Collectors.groupingBy(
            point -> point.method() + " " + point.kind(), Collectors.flatMapping(point -> model.callsOf(point).stream()
                .map(SourceModel.PointCall::address), Collectors.toList())));
        this.listening = model.getPoints().stream().collect(Collectors.groupingBy(
            point -> point.method() + " " + point.kind(), Collectors.mapping(point -> Locality.ofTheCall(point.call())
                .isPresent(), Collectors.reducing(true, Boolean::logicalAnd))))
            .entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    // Where a reach's test would connect. A point whose address is one of its parameters is followed back along the
    // test's chain, to the argument each caller passes, as far as the test.
    ForTests of(final List<MethodKey> chain, final String kind) {
        final ForTests graded = graded(chain, kind);
        if (!RUN_TIME.equals(graded.grade()) || chain.size() < 2) {
            return graded;
        }
        return fromTheTest(chain, kind).orElse(isNeverGivenOne(chain, kind) ? new ForTests(NONE, null) : graded);
    }

    // A client the test makes and never gives an address, whose own address is no setting or other name.
    private boolean isNeverGivenOne(final List<MethodKey> chain, final String kind) {
        return !alongTheChain(chain, byPoint.get(chain.get(chain.size() - 1) + " " + kind)).isNamed()
            && bareClients.isGivenNone(chain);
    }

    // Where the reach's own address leaves it unknown, what the test's call into its chain says, or, failing that, the
    // next step's, such as its set-up's, if that names a host or this machine: an address handed in, held by a builder
    // and read back as the connection is made.
    private Optional<ForTests> fromTheTest(final List<MethodKey> chain, final String kind) {
        return IntStream.range(0, Math.min(2, chain.size() - 1))
            .mapToObj(step -> arguments.atTheTestsCall(chain.get(step), chain.get(step + 1), kind))
            .flatMap(Optional::stream)
            .map(settings::withValue).map(address -> ForTests.of(classify(address, kind), address))
            .filter(found -> HOST.equals(found.grade()) || LOCAL.equals(found.grade())).findFirst();
    }

    private ForTests graded(final List<MethodKey> chain, final String kind) {
        final String point = chain.get(chain.size() - 1) + " " + kind;
        final Address address = alongTheChain(chain, byPoint.get(point));
        if (listening.contains(point) || address.isRelativePath() && servesOnAPort(chain.get(0).type())) {
            return new ForTests(LOCAL, null);
        }
        final Address withValue = settings.withValue(address);
        return ForTests.of(classify(withValue, kind), withValue);
    }

    // The addresses a reach's chain gives its point, for each of its calls whose own address is one of its parameters:
    // what the test's chain passes, as far as it can be followed.
    List<Address> givenAlong(final List<MethodKey> chain, final String kind) {
        return everyCallOf.getOrDefault(chain.get(chain.size() - 1) + " " + kind, List.of()).stream()
            .filter(atThePoint -> ArgumentTracer.parameterOf(atThePoint).isPresent())
            .map(atThePoint -> settings.withValue(alongTheChain(chain, atThePoint))).toList();
    }

    private Address alongTheChain(final List<MethodKey> chain, final Address atThePoint) {
        Address address = atThePoint;
        for (int step = chain.size() - 1; step > 0 && ArgumentTracer.parameterOf(address).isPresent(); step--) {
            final String parameter = ArgumentTracer.parameterOf(address).orElseThrow();
            final Optional<Address> passed = arguments.passedBy(chain.get(step - 1), chain.get(step), parameter);
            if (passed.isEmpty()) {
                break;
            }
            address = passed.get();
        }
        return address;
    }

    // With no settings known on the test classpath, which ones a test runs with is unknown: never "none".
    private String classify(final Address address, final String kind) {
        if (isFilledAtRunTime(address)) {
            return RUN_TIME;
        }
        if (hasNoValue(address)) {
            return noValue();
        }
        return ofItsValue(address, kind);
    }

    private static String ofItsValue(final Address address, final String kind) {
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

    // A setting whose value refers to another, such as ${DB_URL}, with no default: the environment fills it in.
    private static boolean isFilledAtRunTime(final Address address) {
        return Address.SETTING.equals(address.getSource()) && address.getSettingValue() != null
            && SettingAnnotations.keyOf(address.getSettingValue().strip()).isPresent();
    }

    // A setting that no file on the test classpath gives a value, or only a placeholder for one.
    private static boolean hasNoValue(final Address address) {
        return Address.SETTING.equals(address.getSource())
            && (address.getSettingValue() == null || address.isPlaceholder());
    }
}
