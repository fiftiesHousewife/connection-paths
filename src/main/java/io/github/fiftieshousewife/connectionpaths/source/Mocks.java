package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Optional;
import java.util.Set;

/**
 * Recognises a call made on a mock only to stub or verify it, by the names Mockito and its BDD form use:
 * {@code when(store.all())}, {@code given(store.all())}, {@code verify(store).all()},
 * {@code then(store).should().all()} and {@code doReturn(x).when(store).all()}.
 */
public final class Mocks {

    /** Calls whose arguments are being stubbed. */
    private static final Set<String> STUBBING = Set.of("when", "given");
    /** Calls whose result the stubbed or verified call is made on; {@code when} also needs its one argument. */
    private static final Set<String> ON_A_MOCK = Set.of("verify", "should");

    private Mocks() {
    }

    static boolean isStubbedOrVerified(final MethodCallExpr call) {
        return isMadeOnAMock(call) || isStubbed(call);
    }

    private static boolean isMadeOnAMock(final MethodCallExpr call) {
        return call.getScope()
            .filter(MethodCallExpr.class::isInstance)
            .map(MethodCallExpr.class::cast)
            .map(scope -> ON_A_MOCK.contains(scope.getNameAsString())
                || "when".equals(scope.getNameAsString()) && scope.getArguments().size() == 1)
            .orElse(false);
    }

    /** Whether the call is, or is inside, an argument of {@code when(...)} or {@code given(...)}. */
    private static boolean isStubbed(final MethodCallExpr call) {
        Node child = call;
        Optional<Node> parent = call.getParentNode();
        while (parent.isPresent() && parent.get() instanceof Expression) {
            if (parent.get() instanceof MethodCallExpr outer && STUBBING.contains(outer.getNameAsString())
                && outer.getArguments().contains(child)) {
                return true;
            }
            child = parent.get();
            parent = child.getParentNode();
        }
        return false;
    }
}
