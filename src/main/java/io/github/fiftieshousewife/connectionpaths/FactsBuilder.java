package io.github.fiftieshousewife.connectionpaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Turns the source model and call graph into {@link Facts}. */
final class FactsBuilder {

    private static final int REASONS_REPORTED = 25;

    private final SourceModel model;
    private final CallGraph graph;

    FactsBuilder(final SourceModel model, final CallGraph graph) {
        this.model = model;
        this.graph = graph;
    }

    Facts build(final String toolVersion, final List<String> classpathNotes) {
        final Map<String, Set<MethodKey>> startsByKind = new TreeMap<>();
        model.getPoints().forEach(p -> startsByKind.computeIfAbsent(p.kind(), k -> new TreeSet<>()).add(p.method()));
        final Map<String, Map<MethodKey, MethodKey>> reachedByKind = new TreeMap<>();
        startsByKind.forEach((kind, starts) -> reachedByKind.put(kind, graph.reachBack(starts)));
        final List<Facts.TestReach> tests = testsReaching(reachedByKind);
        return Facts.builder()
            .toolVersion(toolVersion)
            .summary(summary(tests.size()))
            .connectionPoints(points())
            .tests(tests)
            .notResolved(model.getUnresolved().top(REASONS_REPORTED))
            .dispatchPointsNotFollowed(graph.getNotFollowed().entrySet().stream()
                .map(e -> new Facts.Dispatch(e.getKey(), e.getValue())).toList())
            .classpathNotes(classpathNotes)
            .build();
    }

    private Facts.Summary summary(final int testsReaching) {
        return Facts.Summary.builder()
            .files(model.getCounts().files)
            .filesNotParsed(model.getCounts().filesNotParsed)
            .methods(model.getDeclared().size())
            .callsResolved(model.getCounts().callsResolved)
            .callsNotResolved(model.getUnresolved().getTotal())
            .tests(model.getTests().size())
            .testsReaching(testsReaching)
            .build();
    }

    private List<Facts.Point> points() {
        return model.getPoints().stream()
            .sorted((a, b) -> (a.method() + a.call()).compareTo(b.method() + b.call()))
            .map(p -> {
                final SourceModel.Location at = model.locationOf(p.method());
                return Facts.Point.builder().kind(p.kind()).call(p.call()).method(p.method().toString())
                    .file(at.file()).line(at.line()).build();
            })
            .toList();
    }

    private List<Facts.TestReach> testsReaching(final Map<String, Map<MethodKey, MethodKey>> reachedByKind) {
        final List<Facts.TestReach> tests = new ArrayList<>();
        model.getTests().forEach((test, at) -> {
            final Facts.TestReach.TestReachBuilder entry = Facts.TestReach.builder()
                .test(test.type() + "." + test.name()).file(at.file()).line(at.line());
            boolean any = false;
            for (final Map.Entry<String, Map<MethodKey, MethodKey>> kind : reachedByKind.entrySet()) {
                final Optional<Facts.Reach> reach = reach(test, kind.getKey(), kind.getValue());
                reach.ifPresent(entry::reach);
                any |= reach.isPresent();
            }
            if (any) {
                tests.add(entry.build());
            }
        });
        return tests;
    }

    /** How a test reaches a kind of connection: through its own body first, otherwise through its set-up. */
    private Optional<Facts.Reach> reach(final MethodKey test, final String kind, final Map<MethodKey, MethodKey> next) {
        if (next.containsKey(test)) {
            return Optional.of(reachOf(kind, CallGraph.chain(test, next), false));
        }
        return model.setUpOf(test.type()).stream()
            .filter(next::containsKey)
            .findFirst()
            .map(setUp -> {
                final List<MethodKey> chain = new ArrayList<>(List.of(test));
                chain.addAll(CallGraph.chain(setUp, next));
                return reachOf(kind, chain, true);
            });
    }

    private static Facts.Reach reachOf(final String kind, final List<MethodKey> chain, final boolean throughSetUp) {
        return Facts.Reach.builder()
            .kind(kind)
            .point(chain.get(chain.size() - 1).toString())
            .throughSetUp(throughSetUp)
            .chain(chain.stream().map(MethodKey::toString).toList())
            .build();
    }
}
