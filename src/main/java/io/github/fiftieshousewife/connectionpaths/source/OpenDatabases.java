package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.address.Declarations;
import io.github.fiftieshousewife.connectionpaths.source.syntax.TypeHeads;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Code that runs only once a database point has connected: a method or constructor given an open connection,
// statement or result set; a call passed a value read from one, such as a row's column; and a call made inside a
// loop or if whose condition reads one, such as while (rows.next()).
final class OpenDatabases {

    private static final Set<String> OPEN = Set.of("java.sql.Connection", "java.sql.Statement",
        "java.sql.PreparedStatement", "java.sql.CallableStatement", "java.sql.ResultSet");

    private OpenDatabases() {
    }

    static boolean given(final NodeWithParameters<?> callable) {
        return callable.getParameters().stream().map(Parameter::getType).anyMatch(OpenDatabases::isOpen);
    }

    static boolean readIn(final Node call) {
        final Set<String> open = variablesAround(call);
        return !open.isEmpty() && (passedOne(call, open) || conditions(call).anyMatch(c -> reads(c, open)));
    }

    private static boolean passedOne(final Node call, final Set<String> open) {
        return call instanceof NodeWithArguments<?> withArguments
            && withArguments.getArguments().stream().anyMatch(argument -> reads(argument, open));
    }

    private static boolean reads(final Node expression, final Set<String> open) {
        return expression.findAll(NameExpr.class).stream().anyMatch(name -> open.contains(name.getNameAsString()));
    }

    // The conditions of the loops and ifs a call is inside.
    private static Stream<Node> conditions(final Node call) {
        return Declarations.ancestors(call).takeWhile(n -> !(n instanceof CallableDeclaration)).flatMap(n -> {
            if (n instanceof WhileStmt loop) {
                return Stream.of(loop.getCondition());
            }
            return n instanceof IfStmt branch ? Stream.of(branch.getCondition()) : Stream.empty();
        });
    }

    // The parameters and locals of the method around a call that hold an open connection, statement or result set.
    private static Set<String> variablesAround(final Node call) {
        return Declarations.enclosingMethod(call).map(callable -> Stream.concat(
                callable.getParameters().stream()
                    .filter(p -> isOpen(p.getType())).map(Parameter::getNameAsString),
                callable.findAll(VariableDeclarator.class).stream()
                    .filter(v -> isOpen(v.getType())).map(VariableDeclarator::getNameAsString))
            .collect(Collectors.toSet())).orElse(Set.of());
    }

    private static boolean isOpen(final Type type) {
        return type instanceof ClassOrInterfaceType named && OPEN.contains(TypeHeads.importedName(named));
    }
}
