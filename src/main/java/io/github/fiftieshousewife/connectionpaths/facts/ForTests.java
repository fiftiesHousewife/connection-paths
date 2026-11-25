package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;

// Where a test would connect, and the host it would connect to, if it names one.
record ForTests(String grade, String host) {

    static ForTests of(final String grade, final Address address) {
        return new ForTests(grade, TestAddresses.HOST.equals(grade) ? PointGroups.hostOf(address).orElse(null) : null);
    }
}
