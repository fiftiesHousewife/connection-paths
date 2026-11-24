package io.github.fiftieshousewife.connectionpaths.rules;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.NameTracer;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Clients that are only an interface in the source, made at run time by a library from its annotations: Feign's
 * {@code @FeignClient}, Retrofit's {@code @GET}, Spring's {@code @HttpExchange}. A call to a method carrying the
 * annotation, or declared in an interface carrying it, is a connection point; its address is the annotation's
 * {@code url}, where it has one.
 */
public final class AnnotatedClients {

    /** The members holding a client's address: Feign's and Spring's {@code url}, MicroProfile's {@code baseUri}. */
    private static final Set<String> URL_MEMBERS = Set.of("url", "baseUri");

    /** A rule matched, and the address the annotation gives. */
    public record Match(ConnectionRules.Rule rule, Address address) {
    }

    private final List<ConnectionRules.Rule> rules;

    public AnnotatedClients(final ConnectionRules rules) {
        this.rules = rules.getRules().stream().filter(ConnectionRules.Rule::isAnnotation).toList();
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    public Optional<Match> match(final ResolvedMethodLikeDeclaration resolved) {
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        try {
            return resolved.toAst().filter(MethodDeclaration.class::isInstance)
                .flatMap(method -> match((MethodDeclaration) method));
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }

    private Optional<Match> match(final MethodDeclaration method) {
        final List<AnnotationExpr> annotations = new ArrayList<>(method.getAnnotations());
        NameTracer.enclosingType(method).ifPresent(type -> annotations.addAll(type.getAnnotations()));
        for (final AnnotationExpr annotation : annotations) {
            final Set<String> names = qualifiedNames(annotation);
            final Optional<ConnectionRules.Rule> rule =
                rules.stream().filter(r -> names.contains(r.type())).findFirst();
            if (rule.isPresent()) {
                return Optional.of(new Match(rule.get(), addressOf(annotation)));
            }
        }
        return Optional.empty();
    }

    /** The names an annotation may stand for: as written if qualified, else through its file's imports and package. */
    static Set<String> qualifiedNames(final AnnotationExpr annotation) {
        final String written = annotation.getNameAsString();
        if (written.contains(".")) {
            return Set.of(written);
        }
        final Set<String> names = new HashSet<>();
        final Optional<CompilationUnit> file = annotation.findCompilationUnit();
        for (final ImportDeclaration imported : file.map(unit -> List.copyOf(unit.getImports())).orElse(List.of())) {
            if (imported.isAsterisk() && !imported.isStatic()) {
                names.add(imported.getNameAsString() + "." + written);
            } else if (imported.getName().getIdentifier().equals(written)) {
                names.add(imported.getNameAsString());
            }
        }
        file.flatMap(CompilationUnit::getPackageDeclaration)
            .ifPresent(declared -> names.add(declared.getNameAsString() + "." + written));
        return names;
    }

    /** The annotation's {@code url} or {@code baseUri}: a {@code ${key}} setting or a literal, else given in code. */
    private static Address addressOf(final AnnotationExpr annotation) {
        final String via = "@" + annotation.getName().getIdentifier();
        final Optional<String> url = annotation instanceof NormalAnnotationExpr normal
            ? normal.getPairs().stream().filter(pair -> URL_MEMBERS.contains(pair.getNameAsString()))
                .map(MemberValuePair::getValue).filter(StringLiteralExpr.class::isInstance)
                .map(value -> ((StringLiteralExpr) value).asString()).findFirst()
            : Optional.empty();
        if (url.isEmpty()) {
            return Address.builtBy("the base URL given where the " + via + " client is made");
        }
        final Matcher setting = NameTracer.PLACEHOLDER.matcher(url.get());
        return setting.matches()
            ? Address.builder().source(Address.SETTING).value(setting.group(1)).via(via).build()
            : Address.of(Address.LITERAL, url.get());
    }
}
