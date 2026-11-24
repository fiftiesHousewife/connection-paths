package io.github.fiftieshousewife.connectionpaths;

import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Spring XML beans whose class is a connection client, such as a {@code DataSource} with a {@code url} property: a
 * connection point the Java source never shows. The point's method is the bean's class and id,
 * {@code org.apache.commons.dbcp.BasicDataSource#<bean dataSource>}, and its file and line are the bean's.
 */
final class SpringBeans {

    /** Properties that hold a client's address, most specific first. */
    private static final List<String> ADDRESS_PROPERTIES = List.of("url", "jdbcUrl", "URL", "serverURL", "serverUrl",
        "brokerURL", "brokerUrl", "providerUrl", "uri", "baseUrl", "serviceUrl", "connectionString", "host",
        "serverName");
    private static final String VIA = "Spring XML";

    private final CombinedTypeSolver solver;
    private final ConnectionRules rules;

    SpringBeans(final CombinedTypeSolver solver, final ConnectionRules rules) {
        this.solver = solver;
        this.rules = rules;
    }

    /** The connection points the beans make, with addresses as the beans give them. */
    List<SourcePoint> points(final List<SpringXml.Bean> beans) {
        final List<SourcePoint> points = new ArrayList<>();
        for (final SpringXml.Bean bean : beans) {
            if (bean.getType() == null) {
                continue;
            }
            rule(bean.getType()).ifPresent(rule -> points.add(new SourcePoint(
                new SourceModel.ConnectionPoint(rule.kind(), rule.toString(), keyOf(bean)),
                new SourceModel.Location(bean.getFile(), bean.getLine()), addressOf(bean))));
        }
        return points;
    }

    /** A connection point, where it is, and its address. */
    record SourcePoint(SourceModel.ConnectionPoint point, SourceModel.Location at, Address address) {
    }

    static MethodKey keyOf(final SpringXml.Bean bean) {
        final String id = bean.getId() == null ? "" : " " + bean.getId();
        return new MethodKey(bean.getType(), "<bean" + id + ">", "");
    }

    /** The first rule naming the bean's class or one of its supertypes. */
    private Optional<ConnectionRules.Rule> rule(final String type) {
        final Set<String> types = typeAndAncestors(type);
        return rules.getRules().stream().filter(rule -> !rule.isAnnotation() && types.contains(rule.type()))
            .findFirst();
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private Set<String> typeAndAncestors(final String type) {
        final Set<String> all = new LinkedHashSet<>(List.of(type));
        try {
            final SymbolReference<ResolvedReferenceTypeDeclaration> solved = solver.tryToSolveType(type);
            if (solved.isSolved()) {
                for (final ResolvedReferenceType ancestor : solved.getCorrespondingDeclaration().getAllAncestors()) {
                    all.add(ancestor.getQualifiedName());
                }
            }
        } catch (RuntimeException | StackOverflowError e) {
            return all;
        }
        return all;
    }

    /** The first address property's value: a {@code ${key}} setting or a literal; else, given at run time. */
    static Address addressOf(final SpringXml.Bean bean) {
        final Optional<String> value = ADDRESS_PROPERTIES.stream().map(bean.getProperties()::get)
            .filter(v -> v != null && !v.isBlank()).findFirst();
        if (value.isEmpty()) {
            return Address.builtBy("the properties of bean " + bean.getId());
        }
        final Matcher setting = NameTracer.PLACEHOLDER.matcher(value.get());
        return setting.matches()
            ? Address.builder().source(Address.SETTING).value(setting.group(1)).via(VIA).build()
            : Address.of(Address.LITERAL, value.get());
    }
}
