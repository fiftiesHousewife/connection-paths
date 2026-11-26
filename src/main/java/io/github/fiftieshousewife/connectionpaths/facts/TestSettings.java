package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.TestModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The settings each test class runs with: those on the test classpath, with its Spring profiles' files before them.
final class TestSettings {

    private final TestModel tests;
    private final SettingsFiles onTheClasspath;
    private final Map<List<String>, SettingsFiles> byProfiles = new ConcurrentHashMap<>();

    TestSettings(final TestModel tests, final SettingsFiles settings) {
        this.tests = tests;
        this.onTheClasspath = settings.onTheTestClasspath();
    }

    SettingsFiles of(final String testClass) {
        return byProfiles.computeIfAbsent(tests.profilesOf(testClass), onTheClasspath::forProfiles);
    }

    boolean isEmpty() {
        return onTheClasspath.isEmpty();
    }
}
