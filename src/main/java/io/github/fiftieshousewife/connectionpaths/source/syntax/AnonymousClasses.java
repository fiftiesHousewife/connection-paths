package io.github.fiftieshousewife.connectionpaths.source.syntax;

import com.github.javaparser.ast.Node;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// The anonymous classes a class makes, named as the compiler names them: Outer$1, Outer$2, and Outer$1$1 for one made
// inside Outer$1. Their methods run when they are called, through the type they implement, not where they are made,
// so their calls belong to them and not to the code around them.
public final class AnonymousClasses {

    /** One anonymous class: its name, and the expression that makes it. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "The syntax tree's node is shared on purpose, as the source's own")
    public record Anonymous(String name, ObjectCreationExpr creation) {
    }

    private AnonymousClasses() {
    }

    // The anonymous classes made directly in a type or in another anonymous class, in the order they are written.
    public static List<Anonymous> madeIn(final Node owner, final String ownerName) {
        final List<ObjectCreationExpr> made = owner.findAll(ObjectCreationExpr.class,
            creation -> creation.getAnonymousClassBody().isPresent() && classAround(creation).orElse(null) == owner);
        return IntStream.range(0, made.size())
            .mapToObj(i -> new Anonymous(ownerName + "$" + (i + 1), made.get(i))).toList();
    }

    // Whether a node is in the body of an anonymous class that is itself inside a member's body.
    public static boolean isInsideAnonymousClassIn(final Node node, final Node body) {
        return ancestors(node).takeWhile(ancestor -> ancestor != body).anyMatch(AnonymousClasses::isAnonymousMember);
    }

    // The nearest named type or anonymous class a node is in.
    private static Optional<Node> classAround(final Node node) {
        return ancestors(node).filter(a -> a instanceof TypeDeclaration || isAnonymousMember(a))
            .findFirst().map(a -> a instanceof TypeDeclaration ? a : a.getParentNode().orElseThrow());
    }

    private static boolean isAnonymousMember(final Node node) {
        return node instanceof BodyDeclaration && node.getParentNode().orElse(null) instanceof ObjectCreationExpr;
    }

    private static Stream<Node> ancestors(final Node node) {
        return Stream.iterate(node.getParentNode(), Optional::isPresent, at -> at.get().getParentNode())
            .map(Optional::get);
    }

    /** The calls of a kind in a member's body, leaving out those in the anonymous classes it makes, its own. */
    public static <T extends Node> Stream<T> callsIn(final Node body, final Class<T> kind) {
        return body.findAll(kind).stream().filter(call -> !isInsideAnonymousClassIn(call, body));
    }
}
