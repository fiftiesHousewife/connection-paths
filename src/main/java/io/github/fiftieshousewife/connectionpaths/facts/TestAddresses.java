package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Locality;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
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

    TestAddresses(final SourceModel model, final SettingsFiles settings) {
        this.settings = settings.onTheTestClasspath();
        this.byPoint = model.getPoints().stream().collect(Collectors.toMap(
            point -> point.method() + " " + point.kind(), model::addressOf, (first, second) -> first));
    }

    String of(final MethodKey point, final String kind) {
        final Address address = settings.withValue(byPoint.get(point + " " + kind));
        return classify(address, kind);
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
