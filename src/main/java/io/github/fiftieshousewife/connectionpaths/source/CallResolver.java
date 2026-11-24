package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.source.syntax.Mocks;
import io.github.fiftieshousewife.connectionpaths.source.syntax.Signatures;
import io.github.fiftieshousewife.connectionpaths.source.syntax.TypeHeads;
import io.github.fiftieshousewife.connectionpaths.source.syntax.AnonymousClasses;
import io.github.fiftieshousewife.connectionpaths.address.AddressTracer;
import io.github.fiftieshousewife.connectionpaths.rules.AnnotatedClients;
import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolves the calls in a member's body, and records each as an edge, a connection point or an unresolved call.
 * Calls in lambdas, anonymous classes and local classes belong to the member that holds them. A call being stubbed or
 * verified on a mock ({@code when(store.all())}, {@code verify(store).all()}) isn't a call, and is left out.
 */
public final class CallResolver {

    private final SourceModel model;
    private final ConnectionRules rules;
    private final Map<String, Map<String, Set<String>>> supertypesOf = new HashMap<>();
    private final AddressTracer addresses = new AddressTracer();
    private final AnnotatedClients clients;

    CallResolver(final SourceModel model, final ConnectionRules rules) {
        this.model = model;
        this.rules = rules;
        this.clients = new AnnotatedClients(rules);
    }

    void collect(final MethodKey caller, final Node body, final String file) {
        AnonymousClasses.callsIn(body, MethodCallExpr.class).filter(call -> !Mocks.isStubbedOrVerified(call))
            .forEach(call -> record(caller, call::resolve, new Site(call, file)));
        AnonymousClasses.callsIn(body, ExplicitConstructorInvocationStmt.class)
            .forEach(call -> record(caller, call::resolve, new Site(call, file)));
        AnonymousClasses.callsIn(body, ObjectCreationExpr.class).forEach(create -> {
            record(caller, create::resolve, new Site(create, file));
            XmlContexts.loadedBy(create).forEach(xml -> model.call(caller, XmlContexts.contextOf(xml)));
            final String created = TypeHeads.qualifiedName(create.getType());
            if (!created.startsWith("?.")) {
                model.createdBy(caller, created);
            }
        });
        AnonymousClasses.callsIn(body, MethodReferenceExpr.class)
            .forEach(reference -> record(caller, reference::resolve, new Site(reference, file)));
    }

    // Where a call is, for the unresolved report.
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
        edge(caller, callee, site.node());
        final Optional<AnnotatedClients.Match> match = rules.match(callee, supertypes(resolved.declaringType()))
            .map(rule -> new AnnotatedClients.Match(rule, addresses.trace(site.node(), rule.kind(), rule.type())))
            .or(() -> clients.match(resolved));
        match.filter(found -> DnsLookups.connects(found.rule(), found.address())).ifPresent(found -> model.point(
            new SourceModel.ConnectionPoint(found.rule().kind(), found.rule().toString(), caller),
            site.location(), found.address()));
    }

    private void edge(final MethodKey caller, final MethodKey callee, final Node site) {
        createdByReference(caller, callee, site);
        model.call(caller, callee);
        if (OpenDatabases.readIn(site)) {
            model.mark(Mark.ON_AN_OPEN_DATABASE, caller + " > " + callee);
        }
        if (site instanceof MethodCallExpr call && call.getScope().filter(Expression::isSuperExpr).isPresent()) {
            model.mark(Mark.NOT_VIRTUAL, caller + " > " + callee);
        }
        model.getCounts().callsResolved++;
    }

    // A constructor reference, X::new, creates objects of X when it is called.
    private void createdByReference(final MethodKey caller, final MethodKey callee, final Node site) {
        if (callee.isConstructor() && site instanceof MethodReferenceExpr) {
            model.createdBy(caller, callee.type());
        }
    }

    static MethodKey keyOf(final ResolvedMethodLikeDeclaration resolved) {
        final boolean constructor = resolved instanceof ResolvedConstructorDeclaration;
        return new MethodKey(resolved.declaringType().getQualifiedName(),
            constructor ? MethodKey.CONSTRUCTOR : resolved.getName(), Signatures.of(resolved));
    }

    // A type's supertypes, each with the names of the methods it declares (empty where they can't be read).
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private Map<String, Set<String>> supertypes(final ResolvedReferenceTypeDeclaration type) {
        return supertypesOf.computeIfAbsent(type.getQualifiedName(), name -> {
            final Map<String, Set<String>> all = new LinkedHashMap<>();
            try {
                for (final ResolvedReferenceType ancestor : type.getAllAncestors()) {
                    all.put(ancestor.getQualifiedName(), methodNames(ancestor));
                }
            } catch (RuntimeException | StackOverflowError e) {
                ancestorsFromSource(name).ifPresent(found -> found.forEach(each -> all.put(each, Set.of())));
            }
            return all;
        });
    }

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static Set<String> methodNames(final ResolvedReferenceType type) {
        try {
            return type.getTypeDeclaration().map(declaration -> declaration.getDeclaredMethods().stream()
                .map(ResolvedMethodDeclaration::getName).collect(Collectors.toSet())).orElse(Set.of());
        } catch (RuntimeException | StackOverflowError e) {
            return Set.of();
        }
    }

    private Optional<Set<String>> ancestorsFromSource(final String type) {
        final Set<String> fromSource = model.types().ancestors(type);
        return fromSource.isEmpty() ? Optional.empty() : Optional.of(fromSource);
    }
}
