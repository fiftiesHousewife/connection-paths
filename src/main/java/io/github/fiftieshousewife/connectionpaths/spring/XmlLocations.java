package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.XmlContexts;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Joins each Spring XML location the code names to the files it stands for: the file at that path on the classpath;
// for a wildcard, such as classpath*:spring/*.xml, each file it matches; and where no file is at the path, any of the
// same name, as a location may be given from somewhere the source doesn't show.
final class XmlLocations {

    private XmlLocations() {
    }

    // A location an XML file imports, from the file's own folder unless it says the classpath.
    static String importedBy(final String file, final String resource) {
        final String path = XmlContexts.pathOf(file);
        final boolean fromTheRoot = resource.contains(":") || resource.startsWith("/");
        final int folder = path.lastIndexOf('/');
        return fromTheRoot || folder < 0 ? resource : path.substring(0, folder + 1) + resource;
    }

    static void join(final SourceModel model, final List<String> files) {
        final Set<String> paths = files.stream().map(XmlContexts::pathOf).collect(Collectors.toSet());
        final Set<String> named = model.getCalls().values().stream().flatMap(callees -> callees.stream())
            .map(XmlContexts::locationOf).flatMap(Optional::stream)
            .filter(location -> !paths.contains(location)).collect(Collectors.toSet());
        named.forEach(location -> paths.stream().filter(path -> standsFor(location, path))
            .forEach(path -> model.xmlContextHolds(location, XmlContexts.contextOf(path))));
    }

    private static boolean standsFor(final String location, final String path) {
        return location.contains("*") ? glob(location).matcher(path).matches() : nameOf(location).equals(nameOf(path));
    }

    // A wildcard as Spring reads one: ** any folders, * any part of a name.
    private static Pattern glob(final String location) {
        final String regex = Pattern.quote(location).replace("**", "\\E.*\\Q").replace("*", "\\E[^/]*\\Q");
        return Pattern.compile(regex);
    }

    private static String nameOf(final String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
