package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// A point whose address is one of its parameters is in the system of what its callers pass, so a test reaching it
// reaches only the system its own chain gives the address of: where the chain passes a host or a name, that one's;
// where it passes nothing that can be followed, the point's class's. A reach that names no system it may reach is
// kept for its point, without a system.
final class GivenSystems {

    private final TestAddresses testAddresses;
    private final Map<String, MethodKey> methods = new HashMap<>();

    GivenSystems(final SourceModel model, final TestAddresses testAddresses) {
        this.testAddresses = testAddresses;
        model.getDeclared().forEach(method -> methods.put(method.toString(), method));
    }

    List<TestFacts.Reach> of(final List<TestFacts.Reach> reaches) {
        final Map<String, List<TestFacts.Reach>> byPoint = new LinkedHashMap<>();
        reaches.forEach(r -> byPoint.computeIfAbsent(r.getKind() + " " + r.getPoint(), k -> new ArrayList<>()).add(r));
        final List<TestFacts.Reach> kept = new ArrayList<>();
        byPoint.values().forEach(atAPoint -> {
            final List<TestFacts.Reach> given = atAPoint.stream().filter(this::isGiven).toList();
            kept.addAll(given.isEmpty() ? List.of(atAPoint.get(0).toBuilder().system(null).build()) : given);
        });
        return kept;
    }

    private boolean isGiven(final TestFacts.Reach reach) {
        if (reach.getSystem() == null) {
            return true;
        }
        final List<Optional<String>> names = namesGiven(reach);
        if (names.isEmpty() || names.contains(Optional.of(reach.getSystem()))) {
            return true;
        }
        return names.stream().allMatch(Optional::isEmpty) && isItsPointsClass(reach);
    }

    // The system names the reach's chain gives its point's calls, for those whose address is one of their parameters.
    private List<Optional<String>> namesGiven(final TestFacts.Reach reach) {
        final List<MethodKey> chain = reach.getChain().stream().map(methods::get).toList();
        final List<Address> given = chain.contains(null) ? List.of() : testAddresses.givenAlong(chain, reach.getKind());
        return given.stream().map(PointGroups::nameOf).toList();
    }

    private static boolean isItsPointsClass(final TestFacts.Reach reach) {
        return reach.getSystem().equals(reach.getPoint().substring(0, reach.getPoint().indexOf('#')));
    }
}
