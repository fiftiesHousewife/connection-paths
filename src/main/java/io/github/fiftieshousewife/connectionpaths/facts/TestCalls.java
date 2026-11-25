package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import java.util.List;
import java.util.Optional;

// Where a test's chain starts: the call in the test, or in its set-up, into the chain's next step, and the test
// class's field it is made on, when it is made on one.
final class TestCalls {

    // The call at the start of a chain, and the field it is made on, if any.
    record FirstCall(TestFacts.CallSite at, String field) {
    }

    private final SourceModel model;
    private final ArgumentTracer calls;

    TestCalls(final SourceModel model) {
        this.model = model;
        this.calls = new ArgumentTracer(model);
    }

    Optional<FirstCall> of(final List<MethodKey> chain, final boolean throughSetUp) {
        final int from = throughSetUp ? 1 : 0;
        if (chain.size() < from + 2) {
            return Optional.empty();
        }
        final MethodKey caller = chain.get(from);
        return calls.declaration(caller).flatMap(body -> siteIn(body, chain.get(from + 1)))
            .map(call -> new FirstCall(TestFacts.CallSite.builder().file(model.locationOf(caller).file())
                .line(call.getBegin().map(p -> p.line).orElse(0)).build(), fieldOf(call).orElse(null)));
    }

    // Where the body goes into the next step: a call to it; for a class's field initialisers, a new of the class;
    // for its static ones, the first use of its name.
    private Optional<Node> siteIn(final Node body, final MethodKey next) {
        final String simple = next.type().substring(next.type().lastIndexOf('.') + 1);
        if (!next.isInitialisers()) {
            return calls.callsIn(body, next).map(Node.class::cast).findFirst();
        }
        return "<fields>".equals(next.name())
            ? body.findFirst(ObjectCreationExpr.class, c -> c.getType().getNameAsString().equals(simple))
                .map(Node.class::cast)
            : body.findFirst(NameExpr.class, n -> n.getNameAsString().equals(simple)).map(Node.class::cast);
    }

    private static Optional<String> fieldOf(final Node call) {
        if (!(call instanceof MethodCallExpr method)) {
            return Optional.empty();
        }
        return method.getScope().flatMap(TestCalls::nameOf).filter(name -> isAFieldOfItsClass(method, name));
    }

    private static Optional<String> nameOf(final Expression scope) {
        if (scope instanceof NameExpr name) {
            return Optional.of(name.getNameAsString());
        }
        return scope instanceof FieldAccessExpr field && field.getScope() instanceof ThisExpr
            ? Optional.of(field.getNameAsString()) : Optional.empty();
    }

    // A field the class declares, which the method doesn't hide with a parameter or local variable of the same name.
    private static boolean isAFieldOfItsClass(final MethodCallExpr call, final String name) {
        final Optional<ClassOrInterfaceDeclaration> owner = enclosing(call, ClassOrInterfaceDeclaration.class);
        final Optional<Node> method = enclosing(call, CallableDeclaration.class).map(Node.class::cast);
        final boolean hidden = method.filter(m -> m.findAll(VariableDeclarator.class).stream()
            .anyMatch(v -> v.getNameAsString().equals(name)) || m.findAll(Parameter.class).stream()
            .anyMatch(p -> p.getNameAsString().equals(name))).isPresent();
        return !hidden && owner.flatMap(type -> type.getFieldByName(name)).isPresent();
    }

    private static <T> Optional<T> enclosing(final Node node, final Class<T> kind) {
        return node.stream(Node.TreeTraversal.PARENTS).filter(kind::isInstance).map(kind::cast).findFirst();
    }
}
