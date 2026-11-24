package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.ForEachStmt;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// The values a for-each loop's variable takes: the elements of the array or list it walks, written out where it is
// made, as in String[] urls = {a, b}; for (String url : urls).
final class Iterated {

    private static final Set<String> LIST_MAKERS = Set.of("of", "asList");

    private Iterated() {
    }

    static Stream<Expression> valuesOf(final Node scope, final String id) {
        return scope.findAll(ForEachStmt.class, loop -> loop.getVariableDeclarator().getNameAsString().equals(id))
            .stream().flatMap(loop -> elementsOf(scope, loop.getIterable()));
    }

    static Stream<Expression> elementsOf(final Node scope, final Expression iterable) {
        if (iterable instanceof NameExpr name) {
            return scope.findAll(VariableDeclarator.class, v -> v.getNameAsString().equals(name.getNameAsString()))
                .stream().map(VariableDeclarator::getInitializer).flatMap(Optional::stream)
                .flatMap(Iterated::written);
        }
        return written(iterable);
    }

    private static Stream<Expression> written(final Expression made) {
        if (made instanceof ArrayInitializerExpr array) {
            return array.getValues().stream();
        }
        if (made instanceof ArrayCreationExpr creation) {
            return creation.getInitializer().stream().flatMap(array -> array.getValues().stream());
        }
        return made instanceof MethodCallExpr call && LIST_MAKERS.contains(call.getNameAsString())
            ? call.getArguments().stream() : Stream.empty();
    }
}
