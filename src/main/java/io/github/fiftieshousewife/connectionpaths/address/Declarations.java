package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Where things in the source are declared: the method or type around a node, and what a call or field resolves to. */
public final class Declarations {

    private Declarations() {
    }

    /** The type declaration nearest around a node. */
    public static Optional<TypeDeclaration<?>> enclosingType(final Node node) {
        return enclosingTypes(node).findFirst();
    }

    static Stream<TypeDeclaration<?>> enclosingTypes(final Node node) {
        return ancestors(node).filter(TypeDeclaration.class::isInstance)
            .<TypeDeclaration<?>>map(n -> (TypeDeclaration<?>) n);
    }

    /** The method, constructor or initialiser nearest around a node. */
    public static Optional<CallableDeclaration<?>> enclosingMethod(final Node node) {
        return ancestors(node).filter(CallableDeclaration.class::isInstance)
            .<CallableDeclaration<?>>map(n -> (CallableDeclaration<?>) n).findFirst();
    }

    // Whether a method has a parameter or local variable of this name, hiding a field of it.
    static boolean declares(final CallableDeclaration<?> method, final String id) {
        return method.getParameterByName(id).isPresent()
            || !method.findAll(VariableDeclarator.class, v -> v.getNameAsString().equals(id)).isEmpty();
    }

    /** The nodes around a node, nearest first. */
    public static Stream<Node> ancestors(final Node node) {
        return Stream.iterate(node.getParentNode(), Optional::isPresent, at -> at.get().getParentNode())
            .map(Optional::get);
    }

    // The text of an expression made only of string literals and constant fields, such as a setting key passed to a
    // method: read without tracing calls, so it costs nothing to try.
    static Optional<String> constant(final Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(literal.asString());
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return constant(binary.getLeft()).flatMap(left -> constant(binary.getRight()).map(right -> left + right));
        }
        return nameOf(expression).flatMap(id -> constantField(expression, id));
    }

    private static Optional<String> nameOf(final Expression expression) {
        if (expression instanceof NameExpr name) {
            return Optional.of(name.getNameAsString());
        }
        return expression instanceof FieldAccessExpr field ? Optional.of(field.getNameAsString()) : Optional.empty();
    }

    private static Optional<String> constantField(final Expression expression, final String id) {
        final Optional<Node> declared = expression instanceof FieldAccessExpr field ? of(field) : Optional.empty();
        return declared.or(() -> fieldDeclaredAround(expression, id))
            .flatMap(node -> node.findFirst(VariableDeclarator.class, v -> v.getNameAsString().equals(id)))
            .flatMap(VariableDeclarator::getInitializer)
            .filter(StringLiteralExpr.class::isInstance)
            .map(e -> ((StringLiteralExpr) e).asString());
    }

    private static Optional<Node> fieldDeclaredAround(final Node node, final String id) {
        return enclosingTypes(node).map(type -> type.getFieldByName(id)).flatMap(Optional::stream).findFirst()
            .map(Node.class::cast);
    }

    static Optional<Node> of(final MethodCallExpr call) {
        return resolved(() -> call.resolve().toAst());
    }

    static Optional<Node> of(final FieldAccessExpr access) {
        return resolved(() -> access.resolve().toAst());
    }

    // Whether a call is to a method the JDK declares, such as String.format or URI.create.
    static boolean declaredInJdk(final MethodCallExpr call) {
        return resolved(() -> Optional.of(call.resolve().declaringType().getQualifiedName()))
            .filter(type -> type.startsWith("java.") || type.startsWith("javax.")).isPresent();
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static <T> Optional<T> resolved(final Supplier<Optional<T>> resolve) {
        try {
            return resolve.get();
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }
}
