package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.List;
import java.util.Map;
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

    TestAddresses(final SourceModel model, final SettingsFiles settings) {
        this.settings = settings.onTheTestClasspath();
        this.arguments = new ArgumentTracer(model);
        this.byPoint = model.getPoints().stream().collect(Collectors.toMap(
            point -> point.method() + " " + point.kind(), model::addressOf, (first, second) -> first));
    }

    // Where a reach's test would connect. A point whose address is one of its parameters is followed back along the
    // test's chain, to the argument each caller passes, as far as the test.
    String of(final List<MethodKey> chain, final String kind) {
        Address address = byPoint.get(chain.get(chain.size() - 1) + " " + kind);
        for (int step = chain.size() - 1; step > 0 && ArgumentTracer.parameterOf(address).isPresent(); step--) {
            final String parameter = ArgumentTracer.parameterOf(address).orElseThrow();
            address = arguments.passedBy(chain.get(step - 1), chain.get(step), parameter).orElse(address);
            if (ArgumentTracer.parameterOf(address).filter(parameter::equals).isPresent()) {
                break;
            }
        }
        return classify(settings.withValue(address), kind);
    }

    private static String classify(final Address address, final String kind) {
        if (hasNoValue(address)) {
            return NONE;
        }
        if (Locality.of(address, kind).isPresent() || "process".equals(kind)) {
            return LOCAL;
        }
        return PointGroups.hostOf(address).isPresent() ? HOST : RUN_TIME;
    }

    // A setting that no file on the test classpath gives a value, or only a placeholder for one.
    private static boolean hasNoValue(final Address address) {
        return Address.SETTING.equals(address.getSource())
            && (address.getSettingValue() == null || address.isPlaceholder());
    }
}
