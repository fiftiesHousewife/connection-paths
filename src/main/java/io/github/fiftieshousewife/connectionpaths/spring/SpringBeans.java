package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.SettingAnnotations;
import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Spring XML beans whose class is a connection client, such as a {@code DataSource} with a {@code url} property: a
 * connection point the Java source never shows. The point's method is the bean's class and id,
 * {@code org.apache.commons.dbcp.BasicDataSource#<bean dataSource>}, and its file and line are the bean's.
 */
public final class SpringBeans {

    private static final List<String> ADDRESS_PROPERTIES = List.of("url", "jdbcUrl", "URL", "serverURL", "serverUrl",
        "brokerURL", "brokerUrl", "providerUrl", "uri", "baseUrl", "serviceUrl", "connectionString", "host",
        "serverName");
    private static final String VIA = "Spring XML";

    private final CombinedTypeSolver solver;
    private final ConnectionRules rules;

    public SpringBeans(final CombinedTypeSolver solver, final ConnectionRules rules) {
        this.solver = solver;
        this.rules = rules;
    }

    /** Adds the beans in the files to the model: the connection points they make, and the classes they create. */
    public void addTo(final SourceModel model, final List<Path> files) {
        final List<SpringXml.Bean> beans = SpringXml.read(files);
        points(beans).forEach(bean -> model.point(bean.point(), bean.at(), bean.address()));
        beans.stream().map(SpringXml.Bean::getType).filter(Objects::nonNull).forEach(model::created);
    }

    // The connection points the beans make, with addresses as the beans give them.
    public List<SourcePoint> points(final List<SpringXml.Bean> beans) {
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
    public record SourcePoint(SourceModel.ConnectionPoint point, SourceModel.Location at, Address address) {
    }

    static MethodKey keyOf(final SpringXml.Bean bean) {
        final String id = bean.getId() == null ? "" : " " + bean.getId();
        return new MethodKey(bean.getType(), "<bean" + id + ">", "");
    }

    // The first rule naming the bean's class or one of its supertypes.
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

    // The first address property's value: a ${key} setting or a literal; else, given at run time.
    static Address addressOf(final SpringXml.Bean bean) {
        final Optional<String> value = ADDRESS_PROPERTIES.stream().map(bean.getProperties()::get)
            .filter(v -> v != null && !v.isBlank()).findFirst();
        return value.map(text -> SettingAnnotations.settingOrLiteral(text, VIA))
            .orElse(Address.builtBy("the properties of bean " + bean.getId()));
    }
}
