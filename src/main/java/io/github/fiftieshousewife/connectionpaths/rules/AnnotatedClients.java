package io.github.fiftieshousewife.connectionpaths.rules;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.Declarations;
import io.github.fiftieshousewife.connectionpaths.address.SettingAnnotations;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clients that are only an interface in the source, made at run time by a library from its annotations: Feign's
 * {@code @FeignClient}, Retrofit's {@code @GET}, Spring's {@code @HttpExchange}. A call to a method carrying the
 * annotation, or declared in an interface carrying it, is a connection point; its address is the annotation's
 * {@code url}, where it has one.
 */
public final class AnnotatedClients {

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
        Declarations.enclosingType(method).ifPresent(type -> annotations.addAll(type.getAnnotations()));
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

    // The names an annotation may stand for: as written if qualified, else through its file's imports and package.
    static Set<String> qualifiedNames(final AnnotationExpr annotation) {
        final String written = annotation.getNameAsString();
        if (written.contains(".")) {
            return Set.of(written);
        }
        final Optional<CompilationUnit> file = annotation.findCompilationUnit();
        final Stream<String> imported = file.stream().flatMap(unit -> unit.getImports().stream())
            .map(declaration -> importedAs(declaration, written)).flatMap(Optional::stream);
        final Stream<String> samePackage = file.flatMap(CompilationUnit::getPackageDeclaration).stream()
            .map(declared -> declared.getNameAsString() + "." + written);
        return Stream.concat(imported, samePackage).collect(Collectors.toSet());
    }

    private static Optional<String> importedAs(final ImportDeclaration imported, final String written) {
        if (imported.isAsterisk() && !imported.isStatic()) {
            return Optional.of(imported.getNameAsString() + "." + written);
        }
        return imported.getName().getIdentifier().equals(written) ? Optional.of(imported.getNameAsString())
            : Optional.empty();
    }

    // The annotation's url or baseUri: a ${key} setting or a literal, else given in code.
    private static Address addressOf(final AnnotationExpr annotation) {
        final String via = "@" + annotation.getName().getIdentifier();
        final Optional<String> url = annotation instanceof NormalAnnotationExpr normal
            ? normal.getPairs().stream().filter(pair -> URL_MEMBERS.contains(pair.getNameAsString()))
                .map(MemberValuePair::getValue).filter(StringLiteralExpr.class::isInstance)
                .map(value -> ((StringLiteralExpr) value).asString()).findFirst()
            : Optional.empty();
        return url.map(text -> SettingAnnotations.settingOrLiteral(text, via))
            .orElse(Address.builtBy("the base URL given where the " + via + " client is made"));
    }
}
