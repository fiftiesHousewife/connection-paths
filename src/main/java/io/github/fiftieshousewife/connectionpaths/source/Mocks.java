package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Recognises a call made on a mock only to stub or verify it, by the names Mockito and its BDD form use:
 * {@code when(store.all())}, {@code given(store.all())}, {@code verify(store).all()},
 * {@code then(store).should().all()} and {@code doReturn(x).when(store).all()}.
 */
public final class Mocks {

    private static final Set<String> STUBBING = Set.of("when", "given");
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

    // Whether the call is, or is inside, an argument of when(...) or given(...).
    private static boolean isStubbed(final MethodCallExpr call) {
        return expressionsAround(call).filter(MethodCallExpr.class::isInstance).map(MethodCallExpr.class::cast)
            .filter(outer -> STUBBING.contains(outer.getNameAsString()))
            .anyMatch(outer -> outer.getArguments().stream().anyMatch(a -> a.equals(call) || a.isAncestorOf(call)));
    }

    // The expressions a node is part of, nearest first, up to the statement that holds them.
    private static Stream<Node> expressionsAround(final Node node) {
        return Stream.iterate(node.getParentNode(), p -> p.filter(Expression.class::isInstance).isPresent(),
            p -> p.get().getParentNode()).map(Optional::get);
    }
}
