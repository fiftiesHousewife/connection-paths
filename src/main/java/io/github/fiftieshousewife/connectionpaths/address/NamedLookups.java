package io.github.fiftieshousewife.connectionpaths.address;

import java.util.Optional;
import java.util.Set;

// A call that reads an address by name: an environment variable, a system property, a classpath resource, a JNDI
// name, or a setting whose dotted key is passed to it.
final class NamedLookups {

    private static final Set<String> RESOURCE_METHODS =
        Set.of("getResource", "getResources", "getResourceAsStream", "getSystemResource", "getSystemResources",
            "getSystemResourceAsStream");

    private NamedLookups() {
    }

    static Optional<Address> lookedUp(final String scope, final String name, final String text) {
        return fromSystem(scope, name, text)
            .or(() -> of(RESOURCE_METHODS.contains(name), Address.CLASSPATH_RESOURCE, text))
            .or(() -> of("lookup".equals(name), Address.JNDI_NAME, text))
            .or(() -> setting(scope, name, text));
    }

    private static Optional<Address> fromSystem(final String scope, final String name, final String text) {
        if (!"System".equals(scope)) {
            return Optional.empty();
        }
        return of("getenv".equals(name), Address.ENVIRONMENT_VARIABLE, text)
            .or(() -> of("getProperty".equals(name), Address.SYSTEM_PROPERTY, text));
    }

    private static Optional<Address> setting(final String scope, final String name, final String text) {
        return AddressText.isSettingKey(text)
            ? Optional.of(Address.builder().source(Address.SETTING).value(text)
                .via(scope.isEmpty() ? name : scope + "." + name).build())
            : Optional.empty();
    }

    private static Optional<Address> of(final boolean matches, final String source, final String text) {
        return matches ? Optional.of(Address.of(source, text)) : Optional.empty();
    }
}
