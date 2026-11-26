package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.SettingAnnotations;
import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.XmlContexts;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The solver is shared on purpose")
    public SpringBeans(final CombinedTypeSolver solver, final ConnectionRules rules) {
        this.solver = solver;
        this.rules = rules;
    }

    /** Adds the beans in the files to the model: the connection points they make, and the classes they create. */
    public void addTo(final SourceModel model, final List<Path> files) {
        final List<SpringXml.Bean> beans = SpringXml.read(files);
        final Map<MethodKey, List<String>> profiles = beans.stream().filter(b -> b.getType() != null)
            .collect(Collectors.toMap(SpringBeans::keyOf, SpringXml.Bean::getProfiles, (first, second) -> first));
        points(beans).forEach(bean -> {
            model.point(bean.point(), bean.at(), bean.address());
            holds(model, bean, profiles.getOrDefault(bean.point().method(), List.of()));
        });
        // A file that imports another makes the imported file's beans too, to any depth.
        final Map<String, List<String>> imports = SpringXml.imports(files);
        imports.forEach((file, imported) -> imported.forEach(resource -> model.xmlContextHolds(
            XmlContexts.contextOf(file), XmlContexts.contextOf(XmlLocations.importedBy(file, resource)))));
        XmlLocations.join(model, List.copyOf(imports.keySet()));
        beans.stream().map(SpringXml.Bean::getType).filter(Objects::nonNull).forEach(model::created);
    }

    // A bean in a profile's block is made through that profile's step, which runs only where the profile is active.
    private static void holds(final SourceModel model, final SourcePoint bean, final List<String> profiles) {
        final MethodKey context = XmlContexts.contextOf(bean.at().file());
        if (profiles.isEmpty()) {
            model.xmlContextHolds(context, bean.point().method());
            return;
        }
        final MethodKey step = XmlProfiles.stepOf(bean.at().file(), profiles);
        model.xmlContextHolds(context, step);
        model.xmlContextHolds(step, bean.point().method());
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

    // A JNDI name for a bean looked up by one; else the first address property's value: a ${key} setting or a literal;
    // else, given at run time.
    static Address addressOf(final SpringXml.Bean bean) {
        final String jndiName = bean.getProperties().get("jndiName");
        if (jndiName != null && !jndiName.isBlank()) {
            return Address.of(Address.JNDI_NAME, jndiName);
        }
        final Optional<String> value = ADDRESS_PROPERTIES.stream().map(bean.getProperties()::get)
            .filter(v -> v != null && !v.isBlank()).findFirst();
        return value.map(text -> SettingAnnotations.settingOrLiteral(text, VIA))
            .orElse(Address.builtBy("the properties of bean " + bean.getId()));
    }
}
