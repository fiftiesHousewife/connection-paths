package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import java.util.Set;

// A DNS context sends nothing as it is made, only when it is asked for records; another directory context, such as
// an LDAP one, binds as it is made, and its look-ups go on the connection it already has.
final class DnsLookups {

    private static final Set<String> LOOKUPS = Set.of("getAttributes", "search");

    private DnsLookups() {
    }

    static boolean connects(final ConnectionRules.Rule rule, final Address address) {
        final boolean dns = Address.DNS.equals(address.getSource());
        if ("directory".equals(rule.kind()) && LOOKUPS.contains(rule.method())) {
            return dns;
        }
        return !(dns && MethodKey.CONSTRUCTOR.equals(rule.method()));
    }
}
