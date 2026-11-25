package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// A client the test makes itself, with no arguments, from a class in the source, and uses for nothing but the call
// into its chain: nothing gives it an address, so it has none to connect to. A class whose code holds a default
// address gives it as a literal, and is never graded as known only at run time; one from a library is not followed.
final class ClientsWithoutAnAddress {

    private final ArgumentTracer calls;
    private final Set<String> sourceTypes;

    ClientsWithoutAnAddress(final SourceModel model) {
        this.calls = new ArgumentTracer(model);
        this.sourceTypes = model.getDeclared().stream().map(MethodKey::type)
            .map(type -> type.substring(type.lastIndexOf('.') + 1)).collect(Collectors.toSet());
    }

    boolean isGivenNone(final List<MethodKey> chain) {
        if (chain.size() < 2) {
            return false;
        }
        return calls.declaration(chain.get(0)).flatMap(body -> calls.callsIn(body, chain.get(1)).findFirst()
                .filter(MethodCallExpr.class::isInstance).map(MethodCallExpr.class::cast)
                .flatMap(MethodCallExpr::getScope).map(scope -> isMadeBareAndOnlyCalled(body, scope)))
            .orElse(false);
    }

    private boolean isMadeBareAndOnlyCalled(final Node body, final Expression scope) {
        if (scope instanceof ObjectCreationExpr creation) {
            return isBare(creation);
        }
        if (!(scope instanceof NameExpr name)) {
            return false;
        }
        final String id = name.getNameAsString();
        final List<VariableDeclarator> declared = body.findAll(VariableDeclarator.class,
            v -> v.getNameAsString().equals(id));
        final long uses = body.findAll(NameExpr.class, n -> n.getNameAsString().equals(id)).size();
        return declared.size() == 1 && uses == 1 && declared.get(0).getInitializer()
            .filter(ObjectCreationExpr.class::isInstance).map(ObjectCreationExpr.class::cast)
            .filter(this::isBare).isPresent();
    }

    private boolean isBare(final ObjectCreationExpr creation) {
        return creation.getArguments().isEmpty() && creation.getAnonymousClassBody().isEmpty()
            && sourceTypes.contains(creation.getType().getNameAsString());
    }
}
