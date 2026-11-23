package io.github.fiftieshousewife.connectionpaths;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves the calls in a member's body, and records each as an edge, a connection point or an unresolved call.
 * Calls in lambdas, anonymous classes and local classes belong to the member that holds them.
 */
final class CallResolver {

    private final SourceModel model;
    private final ConnectionRules rules;
    private final Map<String, Set<String>> supertypesOf = new HashMap<>();

    CallResolver(final SourceModel model, final ConnectionRules rules) {
        this.model = model;
        this.rules = rules;
    }

    void collect(final MethodKey caller, final Node body, final String file) {
        for (final MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            record(caller, () -> call.resolve(), new Site(call, file));
        }
        for (final ObjectCreationExpr create : body.findAll(ObjectCreationExpr.class)) {
            record(caller, () -> create.resolve(), new Site(create, file));
        }
        for (final MethodReferenceExpr reference : body.findAll(MethodReferenceExpr.class)) {
            record(caller, () -> reference.resolve(), new Site(reference, file));
        }
    }

    /** Where a call is, for the unresolved report. */
    private record Site(Node node, String file) {
        SourceModel.Location location() {
            return new SourceModel.Location(file, node.getBegin().map(p -> p.line).orElse(0));
        }
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private void record(final MethodKey caller, final Supplier<ResolvedMethodLikeDeclaration> resolve,
                        final Site site) {
        final ResolvedMethodLikeDeclaration resolved;
        try {
            resolved = resolve.get();
        } catch (RuntimeException | StackOverflowError e) {
            model.getUnresolved().add(e, site.location());
            return;
        }
        final MethodKey callee = keyOf(resolved);
        model.call(caller, callee);
        model.getCounts().callsResolved++;
        rules.match(callee, supertypes(resolved.declaringType()))
            .ifPresent(rule -> model.point(new SourceModel.ConnectionPoint(rule.kind(), rule.toString(), caller)));
    }

    static MethodKey keyOf(final ResolvedMethodLikeDeclaration resolved) {
        final boolean constructor = resolved instanceof ResolvedConstructorDeclaration;
        return new MethodKey(resolved.declaringType().getQualifiedName(),
            constructor ? MethodKey.CONSTRUCTOR : resolved.getName(), Signatures.of(resolved));
    }

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private Set<String> supertypes(final ResolvedReferenceTypeDeclaration type) {
        return supertypesOf.computeIfAbsent(type.getQualifiedName(), name -> {
            final Set<String> all = new LinkedHashSet<>();
            try {
                for (final ResolvedReferenceType ancestor : type.getAllAncestors()) {
                    all.add(ancestor.getQualifiedName());
                }
            } catch (RuntimeException | StackOverflowError e) {
                ancestorsFromSource(name).ifPresent(all::addAll);
            }
            return all;
        });
    }

    private Optional<Set<String>> ancestorsFromSource(final String type) {
        final Set<String> fromSource = model.ancestors(type);
        return fromSource.isEmpty() ? Optional.empty() : Optional.of(fromSource);
    }
}
